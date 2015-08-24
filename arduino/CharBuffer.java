package arduino;

public class CharBuffer
{

  public final static int DEFAULT_SIZE = 1024;

  private char[] _buffer;
  private int _size;
  private int _head = 0;
  private int _tail = 0;
  private int _count = 0;

  public CharBuffer()
  {
    this(DEFAULT_SIZE);
  }

  public CharBuffer(int size)
  {
    _size = size;
    _buffer = new char[size];
  }

  public int count()
  {
    return _count;
  }

  public char get()
  {
    char c = _buffer[_head++];
    if(_head >= _size)
      _head = 0;
    _count--;
    return c;
  }

  public void pushBack(char c)
  {
    if(_count == _size)
      throw new RuntimeException("buffer full");
    _head--;
    if(_head < 0)
      _head = _size - 1;
    _buffer[_head] = c;
  }

  public void put(char c)
  {
    if(_count == _size)
      throw new RuntimeException("buffer full");
    _buffer[_tail++] = c;
    _count++;
    if(_tail >= _size)
      _tail = 0;
  }

  public void put(String s)
  {
    int count = s.length();
    for(int n=0; n<count; n++)
    {
      char c = s.charAt(n);
      put(c);
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    //sb.append("CharBuffer(\"");
    int head = _head;
    while(head != _tail)
    {
      sb.append(_buffer[head++]);
      if(head >= _size)
        head = 0;
    }
    //sb.append("\")");
    return sb.toString();
  }

}
