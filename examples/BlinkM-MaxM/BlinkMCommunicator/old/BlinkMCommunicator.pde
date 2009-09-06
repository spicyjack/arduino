/*
 * BlinkMCommunicator -- Communication gateway between a computer and a BlinkM
 *
 * Command format is:
 * <startbyte><i2c_addr><num_bytes_to_send><num_bytes_to_receive><send_byte0>[<send_byte1>...]
 *
 * Thus minimum command length is 5 bytes long, for reading back a color, e.g.:
 *   {0x01,0x09,0x01,0x01, 'g'}
 * Most commands will be 8 bytes long, say to fade to an RGB color, e.g.:
 *   {0x01,0x09,0x04,0x00, 'f',0xff,0xcc,0x33}
 * The longest command is to write a script line, at 12 bytes long, e.g.:
 *   {0x01,0x09,0x08,0x00, 'W',0x00,0x01,50,'f',0xff,0xcc,0x33}
 * 
 * BlinkM connections to Arduino
 * -----------------------------
 * PWR - -- gnd -- black -- Gnd
 * PWR + -- +5V -- red   -- 5V
 * I2C d -- SDA -- green -- Analog In 4
 * I2C c -- SCK -- blue  -- Analog In 5
 * 
 *
 * Note: This sketch resets the I2C address of the BlinkM.
 *       If you don't want this behavior, comment out "BlinkM_setAddress()"
 *       in setup() and change the variable "blink_addr" to your BlinkM's addr.
 *
 * 2007, Tod E. Kurt, ThingM, http://thingm.com/
 *
 */



#include "Wire.h"
#include "BlinkM_funcs.h"

//#define DEBUG
//#define DEBUG_BLINK

// define this if you're plugging a BlinkM directly into an Arduino,
// into the standard position on analog in pins 2-5
// otherwise you can comment it out.
#define BLINKM_ARDUINO_POWERED 1

#define CMNDR_START_BYTE  0x01
#define CMNDR_END_BYTE    0x02

int blinkm_addr = 0x09;

byte serInStr[32];  // array that will hold the serial input string

int ledPin = 13;

void setup()
{
    pinMode(ledPin, OUTPUT);
    if( BLINKM_ARDUINO_POWERED ) {
        BlinkM_beginWithPower();
    } else {
        BlinkM_begin();
    }
    BlinkM_setAddress( blinkm_addr );  // comment out to not set address
    
    Serial.begin(19200); 
    byte rc = BlinkM_checkAddress( blinkm_addr );
    if( rc == -1 ) 
        Serial.println("no response");
    else if( rc == 1 ) 
        Serial.println("addr mismatch");

    Serial.println("BlinkMCommander ready");
}

void loop()
{
    int num;
    //read the serial port and create a string out of what you read
    if( num = readCommand(serInStr) ) {  // we have a string
      digitalWrite(ledPin,HIGH);  // say we're working on it

#ifdef DEBUG_BLINK
      for( int i=0; i<num; i++ ) {
        digitalWrite(ledPin, HIGH);
        delay(100);
        digitalWrite(ledPin,LOW);
        delay(100);
      }
#endif
        //Serial.print("read numbytes:"); Serial.println(num);
        if( serInStr[0] = CMNDR_START_BYTE ) { // we have a command
            byte addr    = serInStr[1];
            byte sendlen = serInStr[2];
            byte recvlen = serInStr[3];
            byte* cmd    = serInStr+4;
#ifdef DEBUG
            Serial.print(" addr:"); Serial.print(addr,HEX);
            Serial.print(" sendlen:"); Serial.print(sendlen,HEX);
            Serial.print(" cmd[0..6]:"); Serial.print(cmd[0],HEX);
            Serial.print(","); Serial.print(cmd[1],HEX); 
            Serial.print(","); Serial.print(cmd[2],HEX);
            Serial.print(","); Serial.print(cmd[3],HEX);
            Serial.print(","); Serial.print(cmd[4],HEX);
            Serial.print(","); Serial.print(cmd[5],HEX);
            Serial.print(","); Serial.println(cmd[6],HEX);
#endif
            if( sendlen!=0 ) {
                BlinkM_sendCmd(addr, cmd, sendlen);
            }
            if( recvlen!=0 ) {
                byte resp[16];
                int rc = BlinkM_receiveBytes(addr, resp, recvlen);
                for( int i=0; i<recvlen; i++) 
                    Serial.print(resp[i],BYTE);
            }
        } // command
        else {
            Serial.println("not a command");
        }
        for(int i=0; i<30;i++)
          serInStr[i] = 0;  // say we've used the string

        digitalWrite(ledPin,LOW); // show we're done
    } // read
    
}   



//read a string from the serial and store it in an array
//you must supply the array variable
uint8_t readCommand(byte *str)
{
  if(!Serial.available()) 
    return 0;
  
  delay(20);  // wait a little for serial data

  int c = Serial.read();  // get start byte (hopefully)
  if( c != CMNDR_START_BYTE )
      return 0;
  str[0] = c;

  /*
  c = Serial.read();  // get address
  if( c == -1 ) 
      return 0;
  str[1] = c;
  */

  int i = 1;
  while(Serial.available()) {
    str[i] = Serial.read();   // FIXME: doesn't check buffer overrun
    i++;
    delay(1);
  }
  str[i] = 0;  // indicate end of read string
  return i;  // return number of chars read
}


