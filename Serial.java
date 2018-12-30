/*
 *  serial communication
 */
package arduino;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class Serial {
    static InputStream in ;
    static OutputStream out;
    
    static void SendLineToComPort(String text)
    {
        int len = text.length();
        byte[] buffer;
 
        buffer = text.getBytes();
        try {
            out.write(buffer,0,len);
        } catch (IOException ex) {
            System.out.println("\nError in writing!");
        }
     }
    static void SendLineToComPort(byte[] buffer, int len)
    {
 
        try {
            out.write(buffer,0,len);
        } catch (IOException ex) {
            System.out.println("\nError in writing!");
        }
     }


    static int GetLineFromComPort(byte[] buffer,int len)  
    {
        int count = 0;
        final int ENTER = 13;
        int tried = 0;
         try {
            do
            {
            if (in.available() > 0)
            {
              in.read(buffer,count,1);
              if (buffer[count] == ENTER) // enter
              {
                  return count;
              }
              count ++;
            }
            else
                tried++;
            
            if(tried > 1000)
                return 0;
            }
            while( count < len);

        } catch (IOException ex) {
           return 0;
        }
        return count;
    }
    static boolean connect (String portName, int speed)  
    {
        CommPortIdentifier portIdentifier;
        CommPort commPort;
        try {
            portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
            if ( portIdentifier.isCurrentlyOwned() )
            {
                System.out.println("Error: Port is currently in use");
                return false;
            }
            else
            {
                commPort = portIdentifier.open(portName, 10);
                SerialPort serialPort = (SerialPort) commPort;

                serialPort.setSerialPortParams(speed,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);

                in = serialPort.getInputStream();
                out = serialPort.getOutputStream();
                
                try {
                    Thread.sleep(1000L); // 
                } catch (InterruptedException ex) {
                    //
                }
                return true;
            }     
       
        } catch (NoSuchPortException | IOException | PortInUseException | UnsupportedCommOperationException ex) {
            System.out.print("Avaamisessa ongelmia!\n\n");
            return false;
        }
    }
}
