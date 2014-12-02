/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package torniquete;

import gnu.io.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;

public class Communicator implements SerialPortEventListener {

    //passed from main GUI
    GUI2 window = null;

    //for containing the ports that will be found
    private Enumeration ports = null;

    //map the port names to CommPortIdentifiers
    private final HashMap portMap = new HashMap();

    //this is the object that contains the opened port
    private CommPortIdentifier selectedPortIdentifier = null;
    private SerialPort serialPort = null;

    //input and output streams for sending and receiving data
    private InputStream input = null;
    private OutputStream output = null;

    String entrada = "";
    //just a boolean flag that i use for enabling
    //and disabling buttons depending on whether the program
    //is connected to a serial port or not
    private boolean bConnected = false;
    final static int TIMEOUT = 2000;
    final static int SPACE_ASCII = 32;
    final static int DASH_ASCII = 45;
    final static int NEW_LINE_ASCII = 10;
    String logText = "";

    public Communicator(GUI2 window) {
        this.window = window;
    }

    //search for all the serial ports
    //pre: none
    //post: adds all the found ports to a combo box on the GUI
    public void searchForPorts() {
        ports = CommPortIdentifier.getPortIdentifiers();
        while (ports.hasMoreElements()) {
            CommPortIdentifier curPort = (CommPortIdentifier) ports.nextElement();
            if (curPort.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                System.out.println("curPort.getName():" + curPort.getName());
                if (curPort.getName().isEmpty() == false) {
                    window.cboxPorts.addItem(curPort.getName());
                    portMap.put(curPort.getName(), curPort);
                }
            }
        }
    }

    //connect to the selected port in the combo box
    //pre: ports are already found by using the searchForPorts method
    //post: the connected comm port is stored in commPort, otherwise,
    //an exception is generated
    public void connect() {
        String selectedPort = (String) window.cboxPorts.getSelectedItem();
        selectedPortIdentifier = (CommPortIdentifier) portMap.get(selectedPort);
        CommPort commPort = null;
        try {
            commPort = selectedPortIdentifier.open("TigerControlPanel", TIMEOUT);
            serialPort = (SerialPort) commPort;
            setConnected(true);
            logText = selectedPort + " opened successfully.";
            window.keybindingController.toggleControls();
        } catch (PortInUseException e) {
            logText = selectedPort + " is in use. (" + e.toString() + ")";
        } catch (Exception e) {
            logText = "Failed to open " + selectedPort + "(" + e.toString() + ")";
        }
    }

    //open the input and output streams
    //pre: an open port
    //post: initialized intput and output streams for use to communicate data
    public boolean initIOStream() {
        boolean successful = false;
        try {
            input = serialPort.getInputStream();
            output = serialPort.getOutputStream();
            successful = true;
            return successful;
        } catch (IOException e) {
            logText = "I/O Streams failed to open. (" + e.toString() + ")";
            return successful;
        }
    }

    //starts the event listener that knows whenever data is available to be read
    //pre: an open serial port
    //post: an event listener for the serial port that knows when data is recieved
    public void initListener() {
        try {
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
        } catch (TooManyListenersException e) {
            logText = "Too many listeners. (" + e.toString() + ")";
        }
    }

    //disconnect the serial port
    //pre: an open serial port
    //post: clsoed serial port
    public void disconnect() {
        try {
            serialPort.removeEventListener();
            serialPort.close();
            input.close();
            output.close();
            setConnected(false);
            window.keybindingController.toggleControls();
            logText = "Disconnected.";
        } catch (IOException e) {
            logText = "Failed to close " + serialPort.getName() + "(" + e.toString() + ")";
        }
    }

    final public boolean getConnected() {
        return bConnected;
    }

    public void setConnected(boolean bConnected) {
        this.bConnected = bConnected;
    }

    //what happens when data is received
    //pre: serial event is triggered
    //post: processing on the data it reads
    @Override
    public void serialEvent(SerialPortEvent evt) {
        if (evt.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                byte singleData = (byte) input.read();
                if (singleData != NEW_LINE_ASCII) {
                    logText = new String(new byte[]{singleData});
                    entrada += logText;
                    if (entrada.indexOf("S011000000000E") != -1 || entrada.indexOf("S010000000000E") != -1) {
//                        entrada = "";
                        finalizarLlegada(1,1);                        
                    }
                    else if (entrada.indexOf("S006") != -1 && entrada.length() >= 28) {
//                        entrada = "";
                        setearCuentas();                        
                    }
                }
            } catch (IOException e) {
                logText = "Failed to read data. (" + e.toString() + ")";
            }
        }
    }

    //method that can be called to send data
    //pre: open serial port
    //post: data sent to the other device
    public void writeData(int leftThrottle, int rightThrottle, String mensaje) {
        try {
            for (int i = 0; i < mensaje.length(); i++) {
                char c = mensaje.charAt(i);
                int ascii = (int) c;
                output.write(ascii);
            }
            output.flush();
        } catch (IOException e) {
            logText = "Failed to write data. (" + e.toString() + ")";
        }
    }

    private void finalizarLlegada(int input, int output) {
        TorniqueteDAO dao = new TorniqueteDAO();
        int entradaRegistrar = 0;
        int salidaRegistrar = 0;
        if (entrada.indexOf("S011000000000E") != -1)
            salidaRegistrar = 1;
        else if (entrada.indexOf("S010000000000E") != -1)
            entradaRegistrar = 1;
        else if (entrada.indexOf("S006") != -1) {
            entradaRegistrar = input;
            salidaRegistrar = output;
        }
        dao.registrarIO(GUI2.torniquete_id, entradaRegistrar, salidaRegistrar);
        entrada = "";
        dao.desconectar();
    }

    public void resetearContador(int torniquete_id, int reset) {
        TorniqueteDAO dao = new TorniqueteDAO();
        writeData(TIMEOUT, TIMEOUT, "S004000000E22");
        try {
            dao.reset(torniquete_id);
            dao.desconectar();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void bloqueaDesbloquea(int est) {
        String mensaje;
        if (est == 0) {
            mensaje = "S015000000E22";
        } else {
            mensaje = "S016000000E21";
        }
        writeData(TIMEOUT, TIMEOUT, mensaje);
        System.out.println("Enviado: " + mensaje);
    }
    
    public void desbloqueaEntrada() {
        String mensaje = "S009000000E2F";
        writeData(TIMEOUT, TIMEOUT, mensaje);
    }

    public void getcounter() {
        String mensaje = "S006000000E20";
        writeData(TIMEOUT, TIMEOUT, mensaje);
    }
    
    public void setearCuentas() {
        if (entrada.indexOf("S006000") != -1) {
            TorniqueteDAO dao = new TorniqueteDAO();
            int inputs = Integer.parseInt(entrada.substring(13, 19));
            int outputs = Integer.parseInt(entrada.substring(19, 25));
            try {
                ArrayList cantidades = dao.consultarInOut(GUI2.torniquete_id);
                if (cantidades != null) {
                    int entradas = inputs - (Integer) cantidades.get(0);
                    int salidas = outputs - (Integer) cantidades.get(1);
                    if (entradas > 0 || salidas > 0) {
                        System.out.println("Modificando contadores: in -> " + entradas + " out -> " + salidas);
                        finalizarLlegada(entradas, salidas);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dao.desconectar();
        } else 
            System.out.println("Contador de troniquete averiado");
        entrada = "";
    }

    public void temporizador() throws SQLException {
        System.out.println("1");
        long start = System.currentTimeMillis();
        long aux = start;
        while (true) {
            long end = System.currentTimeMillis();
            long res = end - start;
            if (aux != end) {
                aux = end;
                if (res % 1000 == 0) {
                    TorniqueteDAO dao = new TorniqueteDAO();
                    int estado = dao.consultarEstado(GUI2.torniquete_id);
                    int reset = dao.consultarReset(GUI2.torniquete_id);
                    if (estado == -1) {
                        System.out.println("Error en la consulta del estado");
                    } else if (GUI2.estado != estado) {
                        if (estado == 0) {
                            GUI2.estado = 0;
                            window.jLabel1.setText("Torniquete bloqueado");
                            bloqueaDesbloquea(1);
                        } else {
                            GUI2.estado = 1;
                            window.jLabel1.setText("Torniquete desbloqueado");
                            bloqueaDesbloquea(0);
                        }
                    }
                    if (reset == -1) {
                        System.out.println("Error en la consulta del reset");
                    } else if (reset == 1) {
                        resetearContador(GUI2.torniquete_id, reset);
                    }
                    dao.desconectar();
                }
            }
        }
    }

}