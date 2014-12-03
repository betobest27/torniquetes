/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package torniquete;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    static Connection conn;

    public static Connection getConnection() {
        try {
//            String uname = "dpoveda";
//            String pwd = "millos2012";
//            String servidor = "jdbc:mysql://172.16.1.50/n_torniquetes";
            
//            String uname = "enovasoft";
//            String pwd = "enovasoft";
//            String servidor = "jdbc:mysql://172.16.1.50/torniquetes";
			
            String uname = "enova";
            String pwd = "enova";
            String servidor = "jdbc:mysql://192.168.0.17/aventura";

            Class.forName("com.mysql.jdbc.Driver");
            try	{
                conn = DriverManager.getConnection(servidor,uname,pwd);
            } catch (SQLException ex) {
		ex.printStackTrace();
            }
	} catch(ClassNotFoundException e) {
            System.out.println(e);
	}
	return conn;
    }

}