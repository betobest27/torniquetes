/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package torniquete;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TorniqueteDAO {

    private final Connection connection;

    public TorniqueteDAO() {
        connection = Conexion.getConnection();
    }

    public void desconectar() {
        try {
            connection.close();
        } catch (SQLException e) {
            
        }
    }
    
    public void registrarIO(int id, int input, int output) {
        boolean respuesta = false;
        boolean respuesta1 = false;
        boolean respuesta2 = false;
        boolean respuesta3 = false;
        try {
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            if (input > 0) {
                for (int i = 0; i < input; i++) {
                    respuesta = statement.execute("INSERT INTO entradas_salidas(torniquete_id, fecha, tipo) VALUE (" + id + ", NOW(), I)");
                    if (!respuesta)
                        break;
                }
                if (respuesta && output == 0)
                    respuesta1 = true;
            }
            if (output > 0) {
                for (int i = 0; i < output; i++) {
                    respuesta1 = statement.execute("INSERT INTO entradas_salidas(torniquete_id, fecha, tipo) VALUE (" + id + ", NOW(), O)");
                    if (!respuesta1)
                        break;
                }
                if (respuesta1 && input == 0)
                    respuesta = true;
            }
            statement.close();
            if (respuesta && respuesta1) {
                Date fecha = new Date();
                SimpleDateFormat Formateador = new SimpleDateFormat("yyyy-MM-dd");
                String Fecha = Formateador.format(fecha);
                boolean verificador = registrarActualizar(Fecha);
                if (verificador)
                    respuesta2 = contarTodosDias(Fecha, input, output);
                else
                    respuesta2 = inOutTodosDias(Fecha, input, output);
                if (respuesta2) {
                    respuesta3 = addContador(id, input, output);
                }
            }
            if (respuesta3)
                connection.commit();
            else
                connection.rollback();
        } catch (SQLException e) {
        }
    }
    
    public boolean addContador(int id, int input, int output) {
        boolean respuesta = false;
        try {
            Statement statement = connection.createStatement();
            respuesta = statement.execute("UPDATE torniquetes SET centradas = centradas + " + input + ", csalidas = csalidas + " + output + " WHERE id = " + id);
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return respuesta;
    }

    public boolean inOutTodosDias(String fecha, int input, int output) {
        boolean respuesta = false;
        try {
            Statement statement = connection.createStatement();
            respuesta = statement.execute("INSERT INTO entradas_salidas_dias_parques(fecha, entradas, salidas) VALUE ('" + fecha + "'," + input + "," + output + ")");
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return respuesta;
    }

    public boolean contarTodosDias(String fecha, int input, int output) {
        boolean respuesta = false;
        try {
            Statement statement = connection.createStatement();
            respuesta = statement.execute("UPDATE entradas_salidas_dias_parques SET entradas = entradas + " + input + ", salidas = salidas + " + output + " WHERE fecha = '" + fecha + "'");
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return respuesta;
    }

    /**
     * Esta funcion se encarga de dado el codigo de la tarjeta desbloquear el
     * torniquete
     *
     * @param codigo
     * @return 0 Si la terjeta esta registrada en la base de datos 1 si no se
     * encuentra en el sistema -1 si hay un error en la consulta
     */
    public int validarTarjeta(String codigo) { //, String torniquete_id
        System.out.println("code " + codigo);
        String sql = "SELECT id FROM bracelets WHERE cod_barras = '" + codigo +"'";
        int retornar = -1;
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            if (rs != null) {
                if (rs.next()) {
                    retornar = 0;
                } else {
                    retornar = 1;
                }
                rs.close();
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return retornar;
    }

    /**
     * Esta funcion se encarga de verificar si se va a registrar o a actualizar
     * en la base de datos en el registro de entrada salida
     *
     * @param id
     * @return false Si se va a crear un registro nuevo en la base de datos true
     * si se va a actualizar
     */
    public boolean registrarActualizar(String Fecha) {
        String sql = "SELECT id FROM entradas_salidas_dias_parque WHERE fecha = '" + Fecha + "'";
        boolean retornar = false;
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            if (rs != null) {
                if (rs.next()) {
                    retornar = true;
                }
                rs.close();
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return retornar;
    }

    public int consultarEstado(int id) {
        int estado = -1;
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT estado FROM torniquetes WHERE id = " + id);
            if (rs.next()) {
                estado = rs.getInt("estado");
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return estado;
    }

    public int consultarReset(int id) {
        int reset = -1;
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT reset FROM torniquetes WHERE id = " + id);
            if (rs.next()) {
                reset = rs.getInt("reset");
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reset;
    }

    public void actualizarEstado(int id, int estado) throws SQLException {
        if (estado == 0)
            estado = 1;
        else
            estado = 0;
        try {
            Statement statement = connection.createStatement();
            statement.execute("UPDATE torniquetes SET estado = " + estado + " WHERE id = " + id);
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void reset(int id) throws SQLException {
        try {
            Statement statement = connection.createStatement();
            statement.execute("UPDATE torniquetes SET reset = 0, centradas = 0, csalidas = 0 WHERE id = " + id);
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public ArrayList consultarInOut(int id) throws SQLException {
        int entradas = 0;
        int salidas = 0;
        ArrayList cantidades = null;
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT centradas, csalidas FROM torniquetes WHERE id = " + id);
            if (rs.next()) {
                cantidades = new ArrayList();
                entradas = rs.getInt("centradas");
                cantidades.add(entradas);
                salidas = rs.getInt("csalidas");
                cantidades.add(salidas);
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cantidades;
    }
    
}