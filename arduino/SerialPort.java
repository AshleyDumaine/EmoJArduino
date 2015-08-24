package arduino;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TooManyListenersException;

public class SerialPort implements SerialPortEventListener
{

  public static final int DEFAULT_BUFFER_SIZE = 1024;

  private gnu.io.SerialPort _port;
  private String            _portName;
  private InputStream       _in;
  private OutputStream      _out;
  private PrintWriter       _pw;

  private int     _baud   = 115200;
  private int     _parity = gnu.io.SerialPort.PARITY_NONE;
  private int     _data   = gnu.io.SerialPort.DATABITS_8;
  private int     _stop   = gnu.io.SerialPort.STOPBITS_1;
  private int     _flow   = gnu.io.SerialPort.FLOWCONTROL_NONE;
  private boolean _isOpen = false;

  private CharBuffer _buffer = new CharBuffer(DEFAULT_BUFFER_SIZE);

  public SerialPort(String portName)
  {
    _portName = portName;
  }

  public int available()
  {
    return _buffer.count();
  }

  public void close()
  {
    if(_isOpen)
    {
      _port.removeEventListener();
      _port.close();
      _isOpen = false;
    }
  }

  public String getName()
  {
    return this._portName;
  }

  public boolean isOpen()
  {
    return _isOpen;
  }

  public static ArrayList<String> listPortNames()
  {
    Enumeration<?> portEnum = CommPortIdentifier.getPortIdentifiers();
    ArrayList<String> portNames = new ArrayList<String>();
    while(portEnum.hasMoreElements())
    {
      CommPortIdentifier portId = (CommPortIdentifier)portEnum.nextElement();
      String name = portId.getName();
      portNames.add(name);
    }
    return portNames;
  }

  public void open()
  {
    CommPortIdentifier portIdentifier;
    try
    {
      portIdentifier = CommPortIdentifier.getPortIdentifier(_portName);
      if(portIdentifier.isCurrentlyOwned())
        throw new RuntimeException("SerialPort.open: port " + _portName + " is currently in use");
      CommPort commPort = portIdentifier.open("SerialPort.open", 2000);
      if(!(commPort instanceof gnu.io.SerialPort))
        throw new RuntimeException("SerialPort.open: port " + _portName + " is not a serial port");
      _port = (gnu.io.SerialPort)commPort;
      _port.setSerialPortParams(_baud, _data, _stop, _parity);
      _port.setFlowControlMode(_flow);
      _in = _port.getInputStream();
      _out = _port.getOutputStream();
      _pw = new PrintWriter(_out);
      _port.addEventListener(this);
      _port.notifyOnDataAvailable(true);
      _isOpen = true;
    }
    catch(NoSuchPortException exn)
    {
      throw new RuntimeException("SerialPort.open: there is no port having the name " + _portName);
    }
    catch(PortInUseException exn)
    {
      throw new RuntimeException("SerialPort.open: port " + _portName + " is currently in use");
    }
    catch(UnsupportedCommOperationException exn)
    {
      throw new RuntimeException("SerialPort.open: internal error: unsupported settings on port " + _portName);
      // this would be an error in the setSerialPortParams method call above
    }
    catch(IOException e)
    {
      throw new RuntimeException("SerialPort.open: unable to retrieve input/output streams from serial port " + _portName);
    }
    catch(TooManyListenersException exn)
    {
      throw new RuntimeException("SerialPort.open: too many event listeners on port " + _portName);
    }
  }

  public char readChar()
  {
    while(true)
    {
      synchronized(this)
      {
        if(_buffer.count() > 0)
        {
          char c = _buffer.get();
          return c;
        }
        else
        {
          try
          {
            this.wait();
          }
          catch(InterruptedException e)
          {
            throw new RuntimeException("SerialPort.readChar: interrupted while waiting for data on port " + _portName);
          }
        }
      }
    }
  }

  public String readLine()
  {
    StringBuilder sb = new StringBuilder();
    while(true)
    {
      char c = readChar();
      if(c == '\r')
      {
        char c1 = readChar();
        if(c1 != '\n')
          _buffer.pushBack(c1);
        break;
      }
      else if(c == '\n')
        break;
      else
        sb.append(c);
    }
    return sb.toString();
  }

  @Override
  public void serialEvent(SerialPortEvent spev)
  {
    try
    {
      while(_in.available() > 0)
      {
        int chr = _in.read();
        synchronized(this)
        {
          _buffer.put((char)chr);
          this.notify();
        }
      }
    }
    catch(IOException exn)
    {
      throw new RuntimeException("SerialPort.serialEvent: error reading from port " + _portName);
    }    
  }

  public void setBaud(int baudRate)
  {
    this._baud = baudRate;
  }

  public void setDataBits(int dataBits)
  {
    this._data = dataBits;
  }

  public void setFlowControl(int flowControl)
  {
    this._flow = flowControl;
  }

  public void setParity(int parity)
  {
    this._parity = parity;
  }

  public void setStopBits(int stopBits)
  {
    this._stop = stopBits;
  }

  public void write(String string)
  {
    if(!_isOpen)
      throw new RuntimeException("SerialPort.write: port " + _portName + " is not open");
    _pw.print(string);
    _pw.flush();
  }

  public void writeLn(String string)
  {
    if(!_isOpen)
      throw new RuntimeException("SerialPort.writeLn: port " + _portName + " is not open");
    _pw.println(string);
    _pw.flush();
  }

}
