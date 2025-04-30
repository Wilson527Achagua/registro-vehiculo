package gui;

import database.ConexionDB;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.FileOutputStream;
import com.lowagie.text.Font;

public class RegistroVehiculo extends JFrame {

    // Componentes del formulario
    private JComboBox<String> cmbNombreConductor;
    private JComboBox<String> cmbPlacas;
    private JTextField txtCedulaConductor;
    private JComboBox<String> cmbTipoVehiculo;
    
    private JTextField txtPesoTara;
    private JTextField txtPesoBruto;
    private JButton btnGuardar;
    

    public RegistroVehiculo() {
        initComponents();
       // Configuración de la ventana
        setTitle("Registro de Vehículos");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Inicializar componentes
        cmbNombreConductor = new JComboBox<>();
        cmbNombreConductor.setEditable(true);
        cargarConductores();

    txtCedulaConductor = new JTextField();

    cmbTipoVehiculo = new JComboBox<>(new String[]{"Turbo", "Mula"});

    cmbPlacas = new JComboBox<>();
    cmbPlacas.setEditable(true);
    cargarPlacas();

    txtPesoTara = new JTextField();
    txtPesoBruto = new JTextField();
    btnGuardar = new JButton("Guardar Registro");

    setLayout(new GridLayout(8, 2));

    add(new JLabel("Nombre del Conductor:"));
    add(cmbNombreConductor);

    add(new JLabel("Cédula del Conductor:"));
    add(txtCedulaConductor);

    add(new JLabel("Tipo de Vehículo:"));
    add(cmbTipoVehiculo);

    add(new JLabel("Placas del Vehículo:"));
    add(cmbPlacas);

    add(new JLabel("Peso Tara (kg):"));
    add(txtPesoTara);

    add(new JLabel("Peso Bruto (kg):"));
    add(txtPesoBruto);

    add(new JLabel());
    add(btnGuardar);
    
    cmbNombreConductor.addActionListener(e -> {
    String nombreSeleccionado = (String) cmbNombreConductor.getSelectedItem();
    if (nombreSeleccionado != null && !nombreSeleccionado.trim().isEmpty()) {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT conductor_cc FROM vehiculos WHERE conductor_nombre = ? LIMIT 1")) {
            stmt.setString(1, nombreSeleccionado.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    txtCedulaConductor.setText(rs.getString("conductor_cc"));
                } else {
                    txtCedulaConductor.setText("");
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al buscar la cédula: " + ex.getMessage());
        }
    }
});

    btnGuardar.addActionListener(e -> guardarRegistro());
}
        
    

private void cargarConductores() {
    try (Connection conn = ConexionDB.conectar();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT DISTINCT conductor_nombre FROM vehiculos ORDER BY conductor_nombre")) {
        while (rs.next()) {
            cmbNombreConductor.addItem(rs.getString("conductor_nombre"));
        }
    } catch (SQLException ex) {
        System.err.println("Error cargando conductores: " + ex.getMessage());
    }
}

private void cargarPlacas() {
    try (Connection conn = ConexionDB.conectar();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT DISTINCT placas FROM vehiculos ORDER BY placas")) {
        while (rs.next()) {
            cmbPlacas.addItem(rs.getString("placas"));
        }
    } catch (SQLException ex) {
        System.err.println("Error cargando placas: " + ex.getMessage());
    }
}


    // Método para guardar el registro en la base de datos
    private void guardarRegistro() {
    String nombre = ((String) cmbNombreConductor.getEditor().getItem()).trim();
    String placas = ((String) cmbPlacas.getEditor().getItem()).trim();

    if (nombre.isEmpty() || txtCedulaConductor.getText().isEmpty() ||
        placas.isEmpty() || txtPesoTara.getText().isEmpty() || txtPesoBruto.getText().isEmpty()) {
        JOptionPane.showMessageDialog(this, "Todos los campos deben ser llenados.");
        return;
    }

    try (Connection conn = ConexionDB.conectar()) {
        String sql = "INSERT INTO vehiculos (fecha, conductor_nombre, conductor_cc, tipo_vehiculo, placas, peso_tara, peso_bruto, peso_neto) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        
        stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
        stmt.setString(2, nombre);
        stmt.setString(3, txtCedulaConductor.getText());
        stmt.setString(4, (String) cmbTipoVehiculo.getSelectedItem());
        stmt.setString(5, placas);
        stmt.setDouble(6, Double.parseDouble(txtPesoTara.getText()));
        stmt.setDouble(7, Double.parseDouble(txtPesoBruto.getText()));
        double pesoNeto = Double.parseDouble(txtPesoBruto.getText()) - Double.parseDouble(txtPesoTara.getText());
        stmt.setDouble(8, pesoNeto);

        stmt.executeUpdate();
        
        mostrarTiquete(nombre, txtCedulaConductor.getText(), (String) cmbTipoVehiculo.getSelectedItem(), placas,
               Double.parseDouble(txtPesoTara.getText()),
               Double.parseDouble(txtPesoBruto.getText()),
               pesoNeto);

        JOptionPane.showMessageDialog(this, "Registro guardado exitosamente.\nPeso Neto: " + pesoNeto + " kg");
        
        limpiarCampos();
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage());
    }
}
    private void imprimirTiquete(String tiquete) {
    JTextArea area = new JTextArea(tiquete);
    try {
        boolean impreso = area.print();
        if (impreso) {
            JOptionPane.showMessageDialog(this, "Tiquete enviado a la impresora.");
        } else {
            JOptionPane.showMessageDialog(this, "Impresión cancelada.");
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al imprimir: " + e.getMessage());
    }
}
    private void mostrarTiquete(String nombre, String cedula, String tipo, String placas, double tara, double bruto, double neto) {
    String tiquete = """
            ----------------------------------------
                    TIQUETE DE REGISTRO
            ----------------------------------------
            Fecha       : %s
            Conductor   : %s
            Cédula      : %s
            Vehículo    : %s
            Placas      : %s
            Peso Tara   : %.2f kg
            Peso Bruto  : %.2f kg
            Peso Neto   : %.2f kg
            ----------------------------------------
            """.formatted(
            LocalDateTime.now().toString(),
            nombre,
            cedula,
            tipo,
            placas,
            tara,
            bruto,
            neto
    );

    JTextArea area = new JTextArea(tiquete);
    area.setEditable(false);
    area.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14));
    JScrollPane scrollPane = new JScrollPane(area);
    scrollPane.setPreferredSize(new Dimension(400, 300));

    int opcion = JOptionPane.showOptionDialog(
    this, scrollPane, "Tiquete Generado",
    JOptionPane.YES_NO_CANCEL_OPTION,
    JOptionPane.INFORMATION_MESSAGE,
    null,
    new String[]{"Imprimir", "Guardar PDF", "Cerrar"},
    "Imprimir"
);

if (opcion == 0) {
    imprimirTiquete(tiquete);
} else if (opcion == 1) {
    generarPDFTiquete(nombre, cedula, tipo, placas, tara, bruto, neto);
}
}


    // Método para limpiar los campos después de guardar
    private void limpiarCampos() {
    cmbNombreConductor.setSelectedIndex(-1);
    txtCedulaConductor.setText("");
    cmbTipoVehiculo.setSelectedIndex(0);
    cmbPlacas.setSelectedIndex(-1);
    txtPesoTara.setText("");
    txtPesoBruto.setText("");
}
    private void generarPDFTiquete(String nombre, String cedula, String tipo, String placas, double tara, double bruto, double neto) {
    Document documento = new Document();
    try {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File("tiquete_vehiculo.pdf"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        PdfWriter.getInstance(documento, new FileOutputStream(fileChooser.getSelectedFile()));
        documento.open();

        Font font = FontFactory.getFont(FontFactory.COURIER_BOLD, 12);
        Paragraph p = new Paragraph();
        p.setFont(font);
        p.add("---------- TIQUETE DE REGISTRO ----------\n\n");
        p.add("Fecha       : " + LocalDateTime.now() + "\n");
        p.add("Conductor   : " + nombre + "\n");
        p.add("Cédula      : " + cedula + "\n");
        p.add("Vehículo    : " + tipo + "\n");
        p.add("Placas      : " + placas + "\n");
        p.add("Peso Tara   : " + tara + " kg\n");
        p.add("Peso Bruto  : " + bruto + " kg\n");
        p.add("Peso Neto   : " + neto + " kg\n\n");
        p.add("------------------------------------------");

        documento.add(p);
        documento.close();

        JOptionPane.showMessageDialog(this, "PDF generado exitosamente.");
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al generar PDF: " + e.getMessage());
    }
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(RegistroVehiculo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(RegistroVehiculo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(RegistroVehiculo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(RegistroVehiculo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new RegistroVehiculo().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
