package arduino;

import java.util.ArrayList;

public class Arduino
{

  private SerialPort _serialPort;
  private boolean _isConnected = false;

  public Arduino(String portName)
  {
    this(new SerialPort(portName));
  }

  public Arduino(SerialPort port)
  {
    _serialPort = port;
  }

  public int analogRead(int pinNumber)
  {
    String s = "ar" + pinNumber;
    _serialPort.writeLn(s);
    String result = _serialPort.readLine();
    return Integer.parseInt(result);
  }

  public void analogWrite(int pinNumber, int value)
  {
    String s = "aw" + pinNumber + "," + value;
    _serialPort.writeLn(s);
  }

  public void connect()
  {
    _serialPort.setBaud(115200);
    _serialPort.setParity(gnu.io.SerialPort.PARITY_NONE);
    _serialPort.setDataBits(gnu.io.SerialPort.DATABITS_8);
    _serialPort.setStopBits(gnu.io.SerialPort.STOPBITS_1);
    _serialPort.setFlowControl(gnu.io.SerialPort.FLOWCONTROL_NONE);
    _serialPort.open();
    try
    {
      for(int n=0; n<20; n++)
      {
        _serialPort.writeLn("99"); //  writeLn flushes automatically
        Thread.sleep(250);
        if(_serialPort.available() > 0)
        {
          String response = _serialPort.readLine();
          if(response.equals("11"))
          {
            Thread.sleep(250);
            while(_serialPort.available() > 0)
              _serialPort.readChar();
            _isConnected = true;
            return;
          }
          _serialPort.close();
          throw new RuntimeException("Arduino.connect: Arduino board present but running incorrect firmware on port " + _serialPort.getName());
        }
        Thread.sleep(250);
      }
    }
    catch(InterruptedException e)
    {
      throw new RuntimeException("Arduino.connect: interrupted while waiting for response from Arduino on port" + _serialPort.getName());
    }
    _serialPort.close();
    throw new RuntimeException("Arduino.connect: Arduino board present but running incorrect firmware on port " + _serialPort.getName());
  }

  public boolean digitalRead(int pinNumber)
  {
    String s = "dr" + pinNumber;
    _serialPort.writeLn(s);
    String result = _serialPort.readLine();
    char cRes = result.charAt(0);
    return cRes == '0' ? false : true;
  }

  public void digitalWrite(int pinNumber, boolean value)
  {
    int iValue = value ? 1 : 0;
    String s = "dw" + pinNumber + "," + iValue;
    _serialPort.writeLn(s);
  }

  public void disconnect()
  {
    _serialPort.close();
    _isConnected = false;
  }

  public static String guessPortName()
  {
    String osName = System.getProperty("os.name").toLowerCase();
    //System.out.println("OS name = \"" + osName + "\"");
    ArrayList<String> portNames = SerialPort.listPortNames();
    String portName = null;
    if(osName.indexOf("mac") >= 0)
      portName = _guessMacPortName(portNames);
    else if(osName.indexOf("win") >= 0)
      portName = _guessWindowsPortName(portNames);
    else if(osName.indexOf("linux") >= 0)
      portName = _guessLinuxPortName(portNames);
    else
      throw new RuntimeException("GuessPortName.guess: operating system " + osName + " not supported");
    return portName;
  }

  private static String _guessLinuxPortName(ArrayList<String> portNames)
  {
    for(String portName : portNames)
      if(portName.startsWith("/dev/ttyUSB"))
        return portName;
    return null;
  }

  private static String _guessMacPortName(ArrayList<String> portNames)
  {
    for(String portName : portNames)
      if(portName.startsWith("/dev/tty.usbserial") || portName.startsWith("/dev/tty.usbmodem"))
        return portName;
    return null;
  }

  private static String _guessWindowsPortName(ArrayList<String> portNames)
  {
    /*
    // if the for loop below this comment block does not work right,
    // try using the code in this comment block instead
    String portName = null;
    for(int n=1; n<10; n++)
    {
      portName = "COM" + n;
      SerialPort port = new SerialPort(portName);
      try
      {
        port.open();
        port.close();
        break;
      }
      catch(Exception exn)
      {}
    }
    return portName;*/
    for(String portName : portNames)
    {
      SerialPort port = new SerialPort(portName);
      try
      {
        port.open();
        port.close();
        return portName;
      }
      catch(Exception exn)
      {}
    }
    return null;
  }

  public boolean isConnected()
  {
    return _isConnected;
  }

  public void pinMode(int pinNumber, char mode)
  {
    if(mode == 'o')
      mode = 'O';
    else if(mode == 'i')
      mode = 'I';
    String s = "pm" + pinNumber + "," + mode;
    _serialPort.writeLn(s);
  }

}
