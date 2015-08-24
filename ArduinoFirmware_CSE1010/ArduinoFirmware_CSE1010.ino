// Firmware for generic control from Matlab (or any other language),

#define DEBUG false

void setup()
{
  // put all pins into known and harmless high-impedance state
  for(int n=0; n<20; n++)
  {
    pinMode(n, INPUT);
    digitalWrite(n, 0);
  }
  Serial.begin(115200);
}

void loop()
{
  stepper_update(); // allow stepper motors to move if necessary
  if(Serial.available())
    handleChar(Serial.read());
}

void handleChar(char c)
{
  if(c == '\n')
  {
    buf_append(0);
    handleBuffer();
    buf_resetReadPos();
    buf_resetWritePos();
  }
  else
    buf_append(c);
}

void handleBuffer()
{
  char c0 = buf_next();
  char c1 = buf_next();
  //if(c0 >= 'a' && c0 <= 'z') c0 -= 32;
  //if(c1 >= 'a' && c1 <= 'z') c1 -= 32;
  #if DEBUG
    Serial.print("> ");
    Serial.println(buf_getChars());
    Serial.print("c0=");
    Serial.print(c0);
    Serial.print(", c1=");
    Serial.println(c1);
  #endif
  switch(c0)
  {
    case '!': // echo
      echo(c0, c1);
      break;
    case 'a': // analog
      analog_handleCmd(c0, c1);
      break;
    case 'd': // digital
    case 'p': // 'PM' pin mode, handled by digital
      digital_handleCmd(c0, c1);
      break;
    case 's': // stepper motors
      stepper_handleCmd(c0, c1);
      break;
    case 'v': // volatile memory
      vmem_handleCmd(c0, c1);
      break;

    // handled locally
    case '9': // 99 returns the firmware version number
      switch(c1)
      {
        case '9':
          Serial.println("11"); // this is an arbitrary number that I chose
          break;
      }
      break;
  }
}

void echo(char c0, char c1)
{
  Serial.print(c1);
  char c;
  while(c = buf_next())
    Serial.print(c);
  Serial.println();
}

// eof

// analog I/O commands

// note that c0 will usually be unused
void analog_handleCmd(char c0, char c1)
{
  switch(c1)
  {
    case 'r':
      analog_read();
      break;
    case 'w':
      analog_write();
      break;
  }
}

// an analog input channel will return a value in the range 0 - 1023
void analog_read()
{
  int pin = buf_nextNum();
  #if DEBUG
    Serial.print("analog read of pin ");
    Serial.println(pin);
  #endif
  int val = analogRead(pin);
  Serial.println(val);
}

// an analog output channel can accept a value in the range 0 - 255
void analog_write()
{
  int pin   = buf_nextNum();
  int value = buf_nextNum();
  analogWrite(pin, value);
}

// eof

// This is the input buffer used by the main loop.

int  buf_readPos  = 0;
int  buf_writePos = 0;

#define BUFFER_SIZE 129
char buf_chars[128];

void buf_append(char c)
{
  buf_chars[buf_writePos++] = c;
}

void buf_fill(char* chars)
{
  buf_writePos = 0;
  while(*chars)
  {
    buf_chars[buf_writePos++] = *chars;
    chars++;
  }
  buf_chars[buf_writePos] = 0;
  buf_readPos = 0;
}

char* buf_getChars()
{
  return buf_chars;
}

// it is safe to read more characters than are in the buffer: this function will return 0
char buf_next()
{
  if(buf_readPos >= buf_writePos)
    return 0;
  return buf_chars[buf_readPos++];
}

int buf_nextNum()
{
  int num = 0;
  char sign = '+';
  byte state = 0;
  boolean contin = true;
  char c = buf_next();
  if(c == ',' || c == ' ')
    c = buf_next();
  if(c == '-' || c == '+')
  {
    sign = c;
    c = buf_next();
  }
  while(contin)
  {
    if(c >= '0' && c <= '9')
    {
      num = (num * 10) + (c - '0');
      c = buf_next();
    }
    else
      contin = false;
  }
  buf_unget();
  if(sign == '-')
    num = -num;
  return num;
}

void buf_resetReadPos()
{
  buf_readPos = 0;
}

void buf_resetWritePos()
{
  buf_writePos = 0;
}

void buf_unget()
{
  buf_readPos--;
}

// eof

// digital I/O commands

// note that c0 will usually be unused
void digital_handleCmd(char c0, char c1)
{
  switch(c1)
  {
    case 'r':
      digital_read();
      break;
    case 'w':
      digital_write();
      break;
    case 'm':
      digital_pinMode();
      break;
  }
}

void digital_read()
{
  int pin = buf_nextNum();
  int val = digitalRead(pin);
  Serial.println(val);
}

void digital_write()
{
  int pin = buf_nextNum();
  buf_next(); // ignore comma
  char mode = buf_next();
  switch(mode)
  {
    case '0':
      digitalWrite(pin, LOW);
      break;
    case '1':
      digitalWrite(pin, HIGH);
  }
}

void digital_pinMode()
{
  int pin = buf_nextNum();
  buf_next(); // ignore comma
  char mode = buf_next();
  #if DEBUG
    Serial.print("Pin mode of pin ");
    Serial.print(pin);
    Serial.print(" = ");
    Serial.print(mode);
    Serial.print(", ");
    Serial.println((int)mode);
  #endif
  switch(mode)
  {
    case 'I':
      pinMode(pin, INPUT);
      break;
    case 'O':
      pinMode(pin, OUTPUT);
      break;
    case 'U':
      pinMode(pin, INPUT_PULLUP);
  }
}

// eof

// Stepper motor functions
// These functions were written to be used with the 28YBJ-48 stepper motors
// and their associated driver boards, but they will undoubtedly work with other motors.

// Arduino output pins 0 and 1 (Rx, Tx) can't be used as stepper motor control pins.

// note that c0 will usually be unused
void stepper_handleCmd(char c0, char c1)
{
  switch(c1)
  {
    case 'a': // stepper attach
      stepper_attach();
      break;
    case 'd': // step delay
      stepper_stepDelay();
      break;
    case 'm': // stepper move
      stepper_move();
      break;
    case 'w': // wait for stepper
      stepper_wait();
      break;
    case 'f': // stepper off
      stepper_off();
      break;
    case 'n': // stepper on
      stepper_on();
      break;
  }
}

const int NUM_STEPPERS = 2;
const int NUM_PHASES   = 4;

byte phases[NUM_PHASES] =
  { B1100,
    B0110,
    B0011,
    B1001 };

byte          stepperPins[NUM_STEPPERS][4]   = {{0, 0, 0, 0}, {0, 0, 0, 0}};
byte          stepperPhase[NUM_STEPPERS]     = {0, 0};
long          stepperStepsLeft[NUM_STEPPERS] = {0, 0};
unsigned long stepperDelay[NUM_STEPPERS]     = {0, 0};
boolean       stepperWait[NUM_STEPPERS]      = {false, false};

// used for timing the inter-step delay of the motor
unsigned long lastNow[2];

// indicates what pins a motor is connected to
void stepper_attach()
{
  int stepperNumber = buf_nextNum();
  int pin0 = buf_nextNum();
  int pin1 = buf_nextNum();
  int pin2 = buf_nextNum();
  int pin3 = buf_nextNum();
  stepperPins[stepperNumber][0] = pin0;
  stepperPins[stepperNumber][1] = pin1;
  stepperPins[stepperNumber][2] = pin2;
  stepperPins[stepperNumber][3] = pin3;
  pinMode(pin0, OUTPUT);
  pinMode(pin1, OUTPUT);
  pinMode(pin2, OUTPUT);
  pinMode(pin3, OUTPUT);
  stepper_off();
  #if DEBUG
    Serial.print("pin 0 = "); Serial.println(stepperPins[stepperNumber][0]);
    Serial.print("pin 1 = "); Serial.println(stepperPins[stepperNumber][1]);
    Serial.print("pin 2 = "); Serial.println(stepperPins[stepperNumber][2]);
    Serial.print("pin 3 = "); Serial.println(stepperPins[stepperNumber][3]);
  #endif
}

// sets the step delay for a motor
// the delay is in microsecods*100
// a delay of 25 is about optimal
// a shorter delay yields less torque
void stepper_stepDelay()
{
  int stepperNumber = buf_nextNum();
  stepperDelay[stepperNumber] = ((unsigned long)buf_nextNum()) * 100;
}

// switches all motor pins off
void stepper_off()
{
  int stepperNumber = buf_nextNum();
  digitalWrite(stepperPins[stepperNumber][0], 0);
  digitalWrite(stepperPins[stepperNumber][1], 0);
  digitalWrite(stepperPins[stepperNumber][2], 0);
  digitalWrite(stepperPins[stepperNumber][3], 0);
}

// switches motor pins on according to the current motor phase
// useful for locking the motor to its current angle
void stepper_on()
{
  int stepperNumber = buf_nextNum();
  int phaseNumber = stepperPhase[stepperNumber];
  _setPinsAccordingToPhase(stepperNumber, phaseNumber);
}

// causess a motor to start moving
// stepper movement happens asynchronously
// see also cmd_stepper_wait for synchronous motion
void stepper_move()
{
  int stepperNumber = buf_nextNum();
  buf_next();
  stepperStepsLeft[stepperNumber] = buf_nextNum();
  lastNow[stepperNumber] = micros();
}

// causes a message string to be sent over the serial port when the motor finishes moving
// the message has the form
// "OK stepper n"
// where n = 0 or 1
void stepper_wait()
{
  int stepperNumber = buf_nextNum();
  stepperWait[stepperNumber] = true;
}

// performs stepper motion
void _moveStepper(int stepperNumber)
{
  unsigned long now = micros();
  unsigned long diff;
  if(now > lastNow[stepperNumber])
    diff = now - lastNow[stepperNumber];
  else
    diff = 4294967295 - lastNow[stepperNumber] + now;
  if(diff > stepperDelay[stepperNumber])
  {
    int stepsLeft = stepperStepsLeft[stepperNumber];
    int dx = stepsLeft < 0 ? -1 : 1;
    stepperStepsLeft[stepperNumber] = stepsLeft - dx;
    lastNow[stepperNumber] = now;
    int phaseNumber = stepperPhase[stepperNumber];
    phaseNumber += dx;
    if(phaseNumber >= NUM_PHASES)
      phaseNumber = 0;
    if(phaseNumber < 0)
      phaseNumber = NUM_PHASES - 1;
    stepperPhase[stepperNumber] = phaseNumber;
    _setPinsAccordingToPhase(stepperNumber, phaseNumber);
  }
}

void _setPinsAccordingToPhase(int stepperNumber, int phaseNumber)
{
  byte pin0 = stepperPins[stepperNumber][0];
  byte pin1 = stepperPins[stepperNumber][1];
  byte pin2 = stepperPins[stepperNumber][2];
  byte pin3 = stepperPins[stepperNumber][3];

  byte phByte = phases[phaseNumber];
  boolean ph0 = bitRead(phByte, 3);
  boolean ph1 = bitRead(phByte, 2);
  boolean ph2 = bitRead(phByte, 1);
  boolean ph3 = bitRead(phByte, 0);
  
  digitalWrite(pin0, ph0);
  digitalWrite(pin1, ph1);
  digitalWrite(pin2, ph2);
  digitalWrite(pin3, ph3);
}

// Called by the main loop to determine if the stepper motor(s) must be moved.
void stepper_update()
{
  for(int n=0; n<NUM_STEPPERS; n++)
  {
    if(stepperStepsLeft[n] != 0)
      _moveStepper(n);
    else if(stepperWait[n])
    {
      Serial.print("OK stepper ");
      Serial.println(n);
      stepperWait[n] = false;
    }
  }
}

// eof

// volatile memory commands

// note that c0 will usually be unused
void vmem_handleCmd(char c0, char c1)
{
  switch(c1)
  {
    case 'r':
      vmem_read();
      break;
    case 'w':
      vmem_write();
      break;
  }
}

long _mem[26];

void vmem_read()
{
  char c = buf_next();
  int loc = 0;
  if(c >= 'a' && c <= 'z') loc = c - 'a';
  else if(c >= 'A' && c <= 'Z') loc = c - 'A';
  int value = _mem[loc];
  //Serial.print("_mem[");
  //Serial.print(loc);
  //Serial.print("] = ");
  Serial.println(value);
}

void vmem_write()
{
  char c = buf_next();
  int loc = 0;
  if(c >= 'a' && c <= 'z') loc = c - 'a';
  else if(c >= 'A' && c <= 'Z') loc = c - 'A';
  int value = buf_nextNum();
  //Serial.print("_mem[");
  //Serial.print(loc);
  //Serial.print("] = ");
  //Serial.println(value);
  _mem[loc] = value;
}

// eof

