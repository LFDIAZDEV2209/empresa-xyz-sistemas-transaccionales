package co.edu.uniminuto.xyz;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Demo educativa Empresa XYZ: Swing + JDBC + InnoDB; abrir en NetBeans como proyecto Maven. */
public class App extends JFrame {
    private final String url = System.getenv().getOrDefault("XYZ_DB_URL", "jdbc:mysql://localhost:3306/ventas?useSSL=false&serverTimezone=UTC");
    private final String user = System.getenv().getOrDefault("XYZ_DB_USER", "root");
    private final String password = System.getenv().getOrDefault("XYZ_DB_PASSWORD", "");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Producto", "Precio COP", "Existencias"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField qty = new JTextField("1", 5);
    private final JTextField name = new JTextField(14);
    private final JTextField price = new JTextField(8);
    private final JTextField stock = new JTextField(5);

    public App() {
        super("Empresa XYZ · Ventas e inventario");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(850, 550); setLocationRelativeTo(null);
        table.setRowHeight(30); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        var header = new JLabel("  EMPRESA XYZ  /  Control transaccional de ventas");
        header.setFont(new Font("SansSerif", Font.BOLD, 20)); header.setForeground(new Color(24, 53, 103));
        var sale = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sale.add(new JLabel("Cantidad:")); sale.add(qty);
        var sellBtn = new JButton("Registrar venta"); sellBtn.addActionListener(e -> runSafe(this::sell)); sale.add(sellBtn);
        var product = new JPanel(new FlowLayout(FlowLayout.LEFT));
        product.add(new JLabel("Producto:")); product.add(name); product.add(new JLabel("Precio:")); product.add(price);
        product.add(new JLabel("Stock:")); product.add(stock);
        var add = new JButton("Agregar"); add.addActionListener(e -> runSafe(this::addProduct)); product.add(add);
        var update = new JButton("Actualizar seleccionado"); update.addActionListener(e -> runSafe(this::updateProduct)); product.add(update);
        var del = new JButton("Eliminar seleccionado"); del.addActionListener(e -> runSafe(this::deleteProduct)); product.add(del);
        var bottom = new JPanel(new GridLayout(3, 1)); bottom.add(sale); bottom.add(product);
        var reports = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var refresh = new JButton("Actualizar listado"); refresh.addActionListener(e -> runSafe(this::refresh)); reports.add(refresh);
        var export = new JButton("Exportar contabilidad CSV"); export.addActionListener(e -> runSafe(this::exportPending)); reports.add(export);
        bottom.add(reports);
        add(header, BorderLayout.NORTH); add(new JScrollPane(table), BorderLayout.CENTER); add(bottom, BorderLayout.SOUTH);
        runSafe(this::refresh);
    }

    private Connection connect() throws SQLException { return DriverManager.getConnection(url, user, password); }
    @FunctionalInterface private interface Action { void execute() throws Exception; }
    private void runSafe(Action a) { try { a.execute(); } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Operación no completada", JOptionPane.ERROR_MESSAGE);
    }}
    private Long selectedId() {
        int row = table.getSelectedRow();
        if (row < 0) throw new IllegalArgumentException("Seleccione un producto de la tabla.");
        return ((Number)model.getValueAt(table.convertRowIndexToModel(row), 0)).longValue();
    }
    private void refresh() throws Exception {
        model.setRowCount(0);
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("SELECT id,nombre,precio,cantidad_disponible FROM productos ORDER BY id"); ResultSet r = p.executeQuery()) {
            while(r.next()) model.addRow(new Object[]{r.getLong(1),r.getString(2),r.getBigDecimal(3),r.getInt(4)});
        }
    }
    private void addProduct() throws Exception {
        BigDecimal amount = new BigDecimal(price.getText().trim()); int amountStock = Integer.parseInt(stock.getText().trim());
        if (name.getText().isBlank() || amount.signum()<0 || amountStock<0) throw new IllegalArgumentException("Datos inválidos.");
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("INSERT INTO productos(nombre,descripcion,precio,cantidad_disponible) VALUES (?,'',?,?)")) {
            p.setString(1,name.getText().trim());p.setBigDecimal(2,amount);p.setInt(3,amountStock);p.executeUpdate();
        } refresh();
    }
    private void updateProduct() throws Exception {
        long id=selectedId(); BigDecimal amount=new BigDecimal(price.getText().trim());int amountStock=Integer.parseInt(stock.getText().trim());
        if (name.getText().isBlank() || amount.signum()<0 || amountStock<0) throw new IllegalArgumentException("Datos inválidos.");
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("UPDATE productos SET nombre=?,precio=?,cantidad_disponible=? WHERE id=?")) {
            p.setString(1,name.getText().trim());p.setBigDecimal(2,amount);p.setInt(3,amountStock);p.setLong(4,id);p.executeUpdate();
        } refresh();
    }
    private void deleteProduct() throws Exception {
        long id=selectedId();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("DELETE FROM productos WHERE id=?")) {p.setLong(1,id);p.executeUpdate();}
        refresh();
    }
    private void sell() throws Exception {
        long id=selectedId(); int units=Integer.parseInt(qty.getText().trim());
        if(units<=0) throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                BigDecimal amount; int available;
                try (PreparedStatement p = c.prepareStatement("SELECT precio,cantidad_disponible FROM productos WHERE id=? FOR UPDATE")) {
                    p.setLong(1,id);
                    try (ResultSet r=p.executeQuery()) { if(!r.next()) throw new SQLException("Producto no encontrado."); amount=r.getBigDecimal(1);available=r.getInt(2); }
                }
                if(available<units) throw new SQLException("Stock insuficiente: disponibles " + available);
                try (PreparedStatement p=c.prepareStatement("UPDATE productos SET cantidad_disponible=cantidad_disponible-? WHERE id=?")) {
                    p.setInt(1,units);p.setLong(2,id);if(p.executeUpdate()!=1) throw new SQLException("No se actualizó inventario.");
                }
                long saleId;
                try (PreparedStatement p=c.prepareStatement("INSERT INTO ventas(producto_id,cantidad_vendida,precio_venta) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    p.setLong(1,id);p.setInt(2,units);p.setBigDecimal(3,amount);p.executeUpdate();
                    try(ResultSet k=p.getGeneratedKeys()){if(!k.next())throw new SQLException("Sin identificador venta");saleId=k.getLong(1);}
                }
                try (PreparedStatement p=c.prepareStatement("INSERT INTO integracion_contabilidad(venta_id,evento_tipo) VALUES (?,'VENTA_REGISTRADA')")) {
                    p.setLong(1,saleId);p.executeUpdate();
                }
                c.commit();
                JOptionPane.showMessageDialog(this,"Venta #"+saleId+" confirmada. Total: $"+amount.multiply(BigDecimal.valueOf(units)));
            } catch(Exception ex){c.rollback();throw ex;} finally {c.setAutoCommit(true);}
        } refresh();
    }
    private void exportPending() throws Exception {
        Path file=Path.of("contabilidad_pendiente.csv");
        // La exportación es una demostración de integración por archivo; no marca como entregado sin acuse del receptor.
        try(Connection c=connect();PreparedStatement p=c.prepareStatement("SELECT v.id,v.fecha_transaccion,p.nombre,v.cantidad_vendida,v.precio_venta,v.total FROM ventas v JOIN productos p ON p.id=v.producto_id JOIN integracion_contabilidad i ON i.venta_id=v.id WHERE i.exportado_en IS NULL ORDER BY v.id");ResultSet r=p.executeQuery()) {
            StringBuilder b=new StringBuilder("venta_id,fecha,producto,cantidad,precio_unitario,total\n");
            while(r.next()){b.append(r.getLong(1)).append(',').append(r.getTimestamp(2)).append(',')
                .append('"').append(r.getString(3).replace("\"","\"\"")).append('"').append(',')
                .append(r.getInt(4)).append(',').append(r.getBigDecimal(5)).append(',').append(r.getBigDecimal(6)).append('\n');}
            Files.writeString(file,b.toString(),StandardCharsets.UTF_8);
        }
        JOptionPane.showMessageDialog(this,"Archivo generado: "+file.toAbsolutePath()+"\nPendiente importación y confirmación del sistema contable.");
    }
    public static void main(String[] args) {SwingUtilities.invokeLater(() -> new App().setVisible(true));}
}
