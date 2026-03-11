package com.burpia.ui;
import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.Normalizador;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PanelConsola extends JPanel {
    // Constantes de configuración
    // DELAY_ACTUALIZACION_MS (1000ms): Intervalo de actualización del resumen de estadísticas.
    // Balance entre responsividad UI y uso de CPU. Menos de 500ms es innecesario, más de 2000ms se siente lento.
    private static final int DELAY_ACTUALIZACION_MS = 1000;

    private final JTextPane consola;
    private final JCheckBox checkboxAutoScroll;
    private final JButton botonLimpiar;
    private final JLabel etiquetaResumen;
    private final GestorConsolaGUI gestorConsola;
    private final Timer timerActualizacion;
    private final JPanel panelControles;
    private final JPanel panelConsolaWrapper;
    private Consumer<Boolean> manejadorCambioAutoScroll;

    // CONFIABILIDAD: Componentes de búsqueda
    private final JTextField campoBusqueda;
    private final JButton botonBuscar;
    private final JButton botonSiguiente;
    private final JButton botonAnterior;
    private final JLabel etiquetaResultadosBusqueda;
    private final JLabel etiquetaBuscar;

    /**
     * Patrón de búsqueda actual. Null si no hay búsqueda activa.
     * No necesita ser volatile porque todos los accesos están en el EDT.
     */
    private Pattern patronBusqueda = null;

    /**
     * Última posición de búsqueda encontrada.
     * -1 indica que no hay búsqueda activa.
     */
    private int posicionUltimaBusqueda = -1;

    /**
     * Flag para prevenir recursión en el listener del checkbox.
     * Cuando se llama setSelected() programáticamente, el listener
     * se dispararía de nuevo. Este flag previene esta recursión.
     */
    private boolean actualizandoAutoScroll = false;

    /**
     * Última versión de consola conocida. No necesita ser volatile porque
     * todos los accesos están protegidos por el EDT (Event Dispatch Thread).
     */
    private int ultimaVersionConsola = -1;

    /**
     * Constructor de PanelConsola.
     *
     * @SuppressWarnings("this-escape")
     * Justificación: Los listeners almacenados en componentes UI no se ejecutan
     * durante el constructor. El Timer se inicia al final de la construcción,
     * cuando el objeto ya está completamente inicializado.
     */
    @SuppressWarnings("this-escape")
    public PanelConsola(GestorConsolaGUI gestorConsola) {
        this.gestorConsola = Objects.requireNonNull(gestorConsola, "gestorConsola no puede ser null");

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelControles = new JPanel();
        panelControles.setLayout(new BoxLayout(panelControles, BoxLayout.Y_AXIS));
        panelControles.setBorder(UIUtils.crearBordeTitulado(
            I18nUI.Consola.TITULO_CONTROLES(), 12, 16));

        // EFICIENCIA: ESTADÍSTICAS PRIMERO - contexto inmediato para decisiones informadas
        etiquetaResumen = new JLabel(I18nUI.Consola.RESUMEN(0, 0, 0, 0));
        etiquetaResumen.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumen.setToolTipText(I18nUI.Tooltips.Consola.RESUMEN());

        // CONFIABILIDAD: Configuración de visualización (toggle) después de contexto
        checkboxAutoScroll = new JCheckBox(I18nUI.Consola.CHECK_AUTO_SCROLL(), true);
        checkboxAutoScroll.setFont(EstilosUI.FUENTE_ESTANDAR);
        checkboxAutoScroll.setToolTipText(I18nUI.Tooltips.Consola.AUTOSCROLL());

        // Acción de limpieza (después de tener contexto para decisión informada)
        botonLimpiar = new JButton(I18nUI.Consola.BOTON_LIMPIAR());
        botonLimpiar.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiar.setToolTipText(I18nUI.Tooltips.Consola.LIMPIAR());

        // EFICIENCIA: BÚSQUEDA agrupada lógicamente
        etiquetaBuscar = new JLabel(I18nUI.Consola.ETIQUETA_BUSCAR());
        etiquetaBuscar.setFont(EstilosUI.FUENTE_ESTANDAR);
        campoBusqueda = new JTextField(15);
        campoBusqueda.setFont(EstilosUI.FUENTE_MONO);
        campoBusqueda.setToolTipText(I18nUI.Tooltips.Consola.CAMPO_BUSCAR());

        botonBuscar = new JButton(I18nUI.Consola.BOTON_BUSCAR());
        botonBuscar.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonBuscar.setToolTipText(I18nUI.Tooltips.Consola.BUSCAR());

        // EFICIENCIA: Resultados ANTES de navegación - usuario sabe si hay algo que navegar
        etiquetaResultadosBusqueda = new JLabel("");
        etiquetaResultadosBusqueda.setFont(EstilosUI.FUENTE_MONO);

        // Navegación al final (solo útil si hay resultados)
        botonAnterior = new JButton("<");
        botonAnterior.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonAnterior.setToolTipText(I18nUI.Tooltips.Consola.ANTERIOR());
        botonAnterior.setEnabled(false);
        botonAnterior.setPreferredSize(new Dimension(40, 25));

        botonSiguiente = new JButton(">");
        botonSiguiente.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonSiguiente.setToolTipText(I18nUI.Tooltips.Consola.SIGUIENTE());
        botonSiguiente.setEnabled(false);
        botonSiguiente.setPreferredSize(new Dimension(40, 25));

        PanelFilasResponsive panelBotones = new PanelFilasResponsive(
            900,
            8,
            8,
            List.of(
                List.of(etiquetaResumen, checkboxAutoScroll, botonLimpiar),
                List.of(etiquetaBuscar, campoBusqueda, botonBuscar, etiquetaResultadosBusqueda, botonAnterior, botonSiguiente)
            )
        );

        panelControles.add(panelBotones);

        panelConsolaWrapper = new JPanel(new BorderLayout());
        panelConsolaWrapper.setBorder(UIUtils.crearBordeTitulado(
            I18nUI.Consola.TITULO_LOGS(), 12, 16));

        consola = new JTextPane();
        consola.setEditable(false);
        consola.setFont(EstilosUI.FUENTE_TABLA);
        consola.setToolTipText(I18nUI.Tooltips.Consola.AREA_LOGS());
        JScrollPane panelDesplazable = new JScrollPane(consola);
        panelDesplazable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        gestorConsola.establecerConsola(consola);

        // Listeners existentes
        checkboxAutoScroll.addActionListener(e -> {
            if (actualizandoAutoScroll) {
                return;
            }
            UIUtils.ejecutarEnEdt(() -> aplicarAutoScrollEnEdt(checkboxAutoScroll.isSelected(), true));
        });

        botonLimpiar.addActionListener(e -> {
            int total = gestorConsola.obtenerTotalLogs();
            if (total == 0) {
                UIUtils.mostrarInfo(this, I18nUI.Consola.TITULO_INFORMACION(), I18nUI.Consola.MSG_CONSOLA_VACIA());
                return;
            }

            boolean confirmacion = UIUtils.confirmarPregunta(
                this,
                I18nUI.Consola.TITULO_CONFIRMAR_LIMPIEZA(),
                I18nUI.Consola.MSG_CONFIRMAR_LIMPIEZA(total)
            );
            if (confirmacion) {
                gestorConsola.limpiarConsola();
                limpiarBusqueda();
            }
        });

        // CONFIABILIDAD: Listener de búsqueda con validación
        botonBuscar.addActionListener(e -> ejecutarBusqueda());

        campoBusqueda.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    ejecutarBusqueda();
                }
            }
        });

        // Navegación de búsqueda
        botonSiguiente.addActionListener(e -> buscarSiguiente());
        botonAnterior.addActionListener(e -> buscarAnterior());

        timerActualizacion = new Timer(DELAY_ACTUALIZACION_MS, e -> actualizarResumen(false));
        timerActualizacion.start();

        panelConsolaWrapper.add(panelDesplazable, BorderLayout.CENTER);

        add(panelControles, BorderLayout.NORTH);
        add(panelConsolaWrapper, BorderLayout.CENTER);

        aplicarTema();
        gestorConsola.registrarInfo(I18nUI.Consola.LOG_INICIALIZADA());
    }

    /**
     * Ejecuta una búsqueda nueva en el texto de la consola.
     * CONFIABILIDAD: Valida inputs, maneja errores de regex
     */
    private void ejecutarBusqueda() {
        String textoBusqueda = campoBusqueda.getText();

        // CONFIABILIDAD: Validar que no esté vacío
        if (Normalizador.esVacio(textoBusqueda)) {
            limpiarBusqueda();
            return;
        }

        try {
            // CONFIABILIDAD: Compilar regex y manejar excepciones
            patronBusqueda = Pattern.compile(textoBusqueda, Pattern.CASE_INSENSITIVE);
            posicionUltimaBusqueda = -1;

            buscarSiguiente();

        } catch (PatternSyntaxException e) {
            // CONFIABILIDAD: Manejar regex inválido
            patronBusqueda = null;
            posicionUltimaBusqueda = -1;
            actualizarBotonesBusqueda(false);

            UIUtils.mostrarError(this,
                I18nUI.Consola.TITULO_INFORMACION(),
                I18nUI.Consola.MSG_BUSQUEDA_REGEX_INVALIDA(textoBusqueda, e.getMessage()));
        }
    }

    /**
     * Busca la siguiente coincidencia desde la posición actual.
     */
    private void buscarSiguiente() {
        if (patronBusqueda == null) {
            return;
        }

        String texto = obtenerTextoDocumento();

        if (Normalizador.esVacio(texto)) {
            mostrarResultadoNoEncontrado(campoBusqueda.getText());
            return;
        }

        // Buscar desde la posición actual + 1
        int inicio = posicionUltimaBusqueda + 1;
        if (inicio >= texto.length()) {
            inicio = 0; // WRAP: volver al inicio
        }

        java.util.regex.Matcher matcher = patronBusqueda.matcher(texto);
        if (inicio > 0 && matcher.find(inicio)) {
            seleccionarYResaltarCoincidencia(matcher.start(), matcher.end());
        } else if (matcher.find()) {
            // WRAP: buscar desde el inicio
            seleccionarYResaltarCoincidencia(matcher.start(), matcher.end());
        } else {
            mostrarResultadoNoEncontrado(campoBusqueda.getText());
        }
    }

    /**
     * Busca la coincidencia anterior desde la posición actual.
     */
    private void buscarAnterior() {
        if (patronBusqueda == null) {
            return;
        }

        String texto = obtenerTextoDocumento();

        if (Normalizador.esVacio(texto)) {
            mostrarResultadoNoEncontrado(campoBusqueda.getText());
            return;
        }

        // Buscar hacia atrás desde la posición actual - 1
        int fin = posicionUltimaBusqueda - 1;
        if (fin < 0) {
            fin = texto.length() - 1; // WRAP: ir al final
        }

        java.util.regex.Matcher matcher = patronBusqueda.matcher(texto);
        int ultimaPosicion = -1;

        while (matcher.find()) {
            if (matcher.start() <= fin) {
                ultimaPosicion = matcher.start();
            } else {
                break;
            }
        }

        if (ultimaPosicion >= 0) {
            matcher.find(ultimaPosicion);
            seleccionarYResaltarCoincidencia(matcher.start(), matcher.end());
        } else {
            mostrarResultadoNoEncontrado(campoBusqueda.getText());
        }
    }

    /**
     * Selecciona y resalta una coincidencia de búsqueda.
     */
    private void seleccionarYResaltarCoincidencia(int inicio, int fin) {
        posicionUltimaBusqueda = inicio;
        actualizarBotonesBusqueda(true);

        UIUtils.ejecutarEnEdt(() -> {
            try {
                consola.setCaretPosition(fin);
                consola.moveCaretPosition(inicio);
                consola.getCaret().setSelectionVisible(true);
            } catch (IllegalArgumentException e) {
                // Posición inválida, ignorar
            }
        });

        actualizarResultadoBusquedaEncontrada();
    }

    /**
     * Obtiene el texto completo del documento de la consola.
     * EFICIENCIA: Acceso directo al documento
     */
    private String obtenerTextoDocumento() {
        try {
            StyledDocument documento = consola.getStyledDocument();
            return documento.getText(0, documento.getLength());
        } catch (BadLocationException e) {
            return "";
        }
    }

    /**
     * Muestra mensaje de "no encontrado" y limpia estado de búsqueda.
     */
    private void mostrarResultadoNoEncontrado(String texto) {
        posicionUltimaBusqueda = -1;
        actualizarBotonesBusqueda(false);
        etiquetaResultadosBusqueda.setText(I18nUI.Consola.MSG_BUSQUEDA_NO_ENCONTRADA(resumirTextoBusqueda(texto)));
    }

    /**
     * Limpia el estado de búsqueda.
     * CONFIABILIDAD: Resetea todos los campos relacionados con búsqueda
     */
    private void limpiarBusqueda() {
        patronBusqueda = null;
        posicionUltimaBusqueda = -1;
        campoBusqueda.setText("");
        actualizarBotonesBusqueda(false);
        etiquetaResultadosBusqueda.setText("");
        UIUtils.ejecutarEnEdt(() -> {
            consola.select(0, 0);
            consola.getCaret().setSelectionVisible(false);
        });
    }

    /**
     * Actualiza el estado de los botones de navegación de búsqueda.
     */
    private void actualizarBotonesBusqueda(boolean busquedaActiva) {
        botonSiguiente.setEnabled(busquedaActiva);
        botonAnterior.setEnabled(busquedaActiva);
    }

    private void actualizarResumen(boolean forzar) {
        int versionActual = gestorConsola.obtenerVersion();
        if (!forzar && versionActual == ultimaVersionConsola) {
            return;
        }
        ultimaVersionConsola = versionActual;
        UIUtils.ejecutarEnEdt(() -> etiquetaResumen.setText(I18nUI.Consola.RESUMEN(
            gestorConsola.obtenerTotalLogs(),
            gestorConsola.obtenerContadorInfo(),
            gestorConsola.obtenerContadorVerbose(),
            gestorConsola.obtenerContadorError()
        )));
    }

    /**
     * Aplica el idioma configurado a todos los elementos de la interfaz del panel.
     * Debe llamarse cuando se cambia el idioma de la configuración.
     */
    public void aplicarIdioma() {
        checkboxAutoScroll.setText(I18nUI.Consola.CHECK_AUTO_SCROLL());
        botonLimpiar.setText(I18nUI.Consola.BOTON_LIMPIAR());
        etiquetaBuscar.setText(I18nUI.Consola.ETIQUETA_BUSCAR());
        botonBuscar.setText(I18nUI.Consola.BOTON_BUSCAR());
        UIUtils.actualizarTituloPanel(panelControles, I18nUI.Consola.TITULO_CONTROLES());
        UIUtils.actualizarTituloPanel(panelConsolaWrapper, I18nUI.Consola.TITULO_LOGS());
        checkboxAutoScroll.setToolTipText(I18nUI.Tooltips.Consola.AUTOSCROLL());
        botonLimpiar.setToolTipText(I18nUI.Tooltips.Consola.LIMPIAR());
        botonBuscar.setToolTipText(I18nUI.Tooltips.Consola.BUSCAR());
        botonAnterior.setToolTipText(I18nUI.Tooltips.Consola.ANTERIOR());
        botonSiguiente.setToolTipText(I18nUI.Tooltips.Consola.SIGUIENTE());
        campoBusqueda.setToolTipText(I18nUI.Tooltips.Consola.CAMPO_BUSCAR());
        etiquetaResumen.setToolTipText(I18nUI.Tooltips.Consola.RESUMEN());
        consola.setToolTipText(I18nUI.Tooltips.Consola.AREA_LOGS());
        aplicarTema();
        refrescarResultadoBusqueda();
        actualizarResumen(true);
        revalidate();
        repaint();
    }

    /**
     * Aplica el tema visual (claro/oscuro) a todos los componentes del panel.
     * Ajusta colores de fondo, texto y bordes según el tema actual.
     */
    public void aplicarTema() {
        UIUtils.ejecutarEnEdt(() -> {
            Color fondoPanel = EstilosUI.obtenerFondoPanel();
            Color fondoConsola = UIManager.getColor("TextPane.background");
            if (fondoConsola == null) {
                fondoConsola = EstilosUI.colorFondoSecundario(fondoPanel);
            }

            setBackground(fondoPanel);
            panelControles.setBackground(fondoPanel);
            panelConsolaWrapper.setBackground(fondoPanel);
            checkboxAutoScroll.setOpaque(false);

            consola.setBackground(fondoConsola);
            consola.setForeground(EstilosUI.colorTextoPrimario(fondoConsola));
            consola.setCaretColor(EstilosUI.colorTextoPrimario(fondoConsola));

            etiquetaResumen.setForeground(EstilosUI.colorTextoSecundario(fondoPanel));
            etiquetaBuscar.setForeground(EstilosUI.colorTextoSecundario(fondoPanel));
            etiquetaResultadosBusqueda.setForeground(EstilosUI.colorTextoSecundario(fondoPanel));

            panelControles.setBorder(UIUtils.crearBordeTitulado(I18nUI.Consola.TITULO_CONTROLES(), 12, 16));
            panelConsolaWrapper.setBorder(UIUtils.crearBordeTitulado(I18nUI.Consola.TITULO_LOGS(), 12, 16));
            gestorConsola.aplicarTemaConsola(consola);

            revalidate();
            repaint();
        });
    }

    /**
     * Libera los recursos asociados al panel de consola.
     * Detiene el timer de actualización de estadísticas.
     */
    public void destruir() {
        timerActualizacion.stop();
    }

    /**
     * Obtiene el gestor de consola GUI asociado a este panel.
     *
     * @return el gestor de consola para agregar logs programáticamente
     */
    public GestorConsolaGUI obtenerGestorConsola() {
        return gestorConsola;
    }

    /**
     * Establece un callback que se invoca cuando el usuario cambia el estado del auto-scroll.
     *
     * @param manejadorCambioAutoScroll consumidor que recibe true si auto-scroll está activo
     */
    public void establecerManejadorCambioAutoScroll(Consumer<Boolean> manejadorCambioAutoScroll) {
        this.manejadorCambioAutoScroll = manejadorCambioAutoScroll;
    }

    /**
     * Establece el estado del auto-scroll programáticamente.
     * Este método es thread-safe y puede llamarse desde cualquier hilo.
     *
     * @param activo true para activar auto-scroll, false para desactivarlo
     */
    public void establecerAutoScrollActivo(boolean activo) {
        UIUtils.ejecutarEnEdtYEsperar(() -> aplicarAutoScrollEnEdt(activo, false));
    }

    /**
     * Obtiene el estado actual del auto-scroll.
     *
     * @return true si auto-scroll está activo, false en caso contrario
     */
    public boolean isAutoScrollActivo() {
        return checkboxAutoScroll.isSelected();
    }

    private void aplicarAutoScrollEnEdt(boolean activo, boolean notificarCambio) {
        boolean estadoActual = checkboxAutoScroll.isSelected();
        boolean cambioEstado = estadoActual != activo;

        if (cambioEstado) {
            iniciarActualizacionAutoScroll();
            try {
                checkboxAutoScroll.setSelected(activo);
            } finally {
                finalizarActualizacionAutoScroll();
            }
        }

        gestorConsola.establecerAutoScroll(activo);

        if (notificarCambio && cambioEstado && manejadorCambioAutoScroll != null) {
            manejadorCambioAutoScroll.accept(activo);
        }
    }

    private void iniciarActualizacionAutoScroll() {
        actualizandoAutoScroll = true;
    }

    private void finalizarActualizacionAutoScroll() {
        actualizandoAutoScroll = false;
    }

    private void refrescarResultadoBusqueda() {
        if (patronBusqueda == null || Normalizador.esVacio(campoBusqueda.getText())) {
            etiquetaResultadosBusqueda.setText("");
            actualizarBotonesBusqueda(false);
            return;
        }

        String texto = obtenerTextoDocumento();
        if (Normalizador.esVacio(texto)) {
            mostrarResultadoNoEncontrado(campoBusqueda.getText());
            return;
        }

        int coincidencias = contarCoincidencias(patronBusqueda, texto);
        if (coincidencias == 0) {
            mostrarResultadoNoEncontrado(campoBusqueda.getText());
            return;
        }

        actualizarBotonesBusqueda(true);
        etiquetaResultadosBusqueda.setText(I18nUI.Consola.MSG_BUSQUEDA_ENCONTRADA(
            coincidencias,
            resumirTextoBusqueda(campoBusqueda.getText())
        ));
    }

    private void actualizarResultadoBusquedaEncontrada() {
        String texto = obtenerTextoDocumento();
        int coincidencias = contarCoincidencias(patronBusqueda, texto);
        if (coincidencias <= 0) {
            mostrarResultadoNoEncontrado(campoBusqueda.getText());
            return;
        }
        etiquetaResultadosBusqueda.setText(I18nUI.Consola.MSG_BUSQUEDA_ENCONTRADA(
            coincidencias,
            resumirTextoBusqueda(campoBusqueda.getText())
        ));
    }

    private int contarCoincidencias(Pattern patron, String texto) {
        if (patron == null || Normalizador.esVacio(texto)) {
            return 0;
        }

        java.util.regex.Matcher matcher = patron.matcher(texto);
        int coincidencias = 0;
        while (matcher.find()) {
            coincidencias++;
        }
        return coincidencias;
    }

    private String resumirTextoBusqueda(String textoBusqueda) {
        if (textoBusqueda == null) {
            return "";
        }
        if (textoBusqueda.length() <= 30) {
            return textoBusqueda;
        }
        return textoBusqueda.substring(0, 30) + "...";
    }

}
