/*
 * BlinkM non-volatile data
 *
 */


// define in makefile so each gets their own
#ifndef I2C_ADDR
#define I2C_ADDR 0x09
#endif

// possible values for boot_mode
#define BOOT_NOTHING     0
#define BOOT_PLAY_SCRIPT 1
#define BOOT_MODE_END    2

#define MAX_EE_SCRIPT_LEN 49

typedef struct _script_line {
    uint8_t dur; 
    uint8_t cmd[4];    // cmd,arg1,arg2,arg3
} script_line;

typedef struct _script {
    uint8_t len;  // number of script lines, 0 == blank script, not playing
    uint8_t reps; // number of times to repeat, 0 == infinite playes
    script_line lines[];
} script;


/*
const uint8_t fillerstart[] PROGMEM = { 0xCA,0xFE,0xBA,0xBE,0x00 };
const uint8_t filler[] PROGMEM =  {
    0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,
    0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f,
    0x10,0x11,0x12,0x13,0x14,0x15,0x16,0x17,
    0x18,0x19,0x1a,0x1b,0x1c,0x1d,0x1e,0x1f,
    0x20,0x21,0x22,0x23,0x24,0x25,0x26,0x27,
    0x28,0x29,0x2a,0x2b,0x2c,0x2d,0x2e,0x2f,
    0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,
    0x38,0x39,0x3a,0x3b,0x3c,0x3d,0x3e,0x3f,
};
const uint8_t fillerend[] PROGMEM = { 0xDE,0xAD,0xBE,0xEF };
*/

// R,G,B,R,G,B,....
const script fl_script_rgb PROGMEM = {
    3, // number of lines
    0, // number of repeats
    {
        { 50, {'c', 0xff,0x00,0x00}},
        { 50, {'c', 0x00,0xff,0x00}},
        { 50, {'c', 0x00,0x00,0xff}},
    }
};
// white blink on & off
const script fl_script_blink_white PROGMEM = {
    2, // number of lines
    0, // number of repeats
    {
        { 20, {'c', 0xff,0xff,0xff}},
        { 20, {'c', 0x00,0x00,0x00}},
    }
};
// red blink on & off
const script fl_script_blink_red PROGMEM = {
    2, // number of lines
    0, // number of repeats
    {
        { 20, {'c', 0xff,0x00,0x00}},
        { 20, {'c', 0x00,0x00,0x00}},
    }
};
// green blink on & off
const script fl_script_blink_green PROGMEM = {
    2, // number of lines
    0, // number of repeats
    {
        { 20, {'c', 0x00,0xff,0x00}},
        { 20, {'c', 0x00,0x00,0x00}},
    }
};
// blue blink on & off
const script fl_script_blink_blue PROGMEM = {
    2, // number of lines
    0, // number of repeats
    {
        { 20, {'c', 0x00,0x00,0xff}},
        { 20, {'c', 0x00,0x00,0x00}},
    }
};
// cyan blink on & off
const script fl_script_blink_cyan PROGMEM = {
    2, // number of lines
    0, // number of repeats
    {
        { 20, {'c', 0x00,0xff,0xff}},
        { 20, {'c', 0x00,0x00,0x00}},
    }
};
// magenta blink on & off
const script fl_script_blink_magenta PROGMEM = {
    2, // number of lines
    0, // number of repeats
    {
        { 20, {'c', 0xff,0x00,0xff}},
        { 20, {'c', 0x00,0x00,0x00}},
    }
};
// yellow blink on & off
const script fl_script_blink_yellow PROGMEM = {
    2, // number of lines
    0, // number of repeats
    {
        { 20, {'c', 0xff,0xff,0x00}},
        { 20, {'c', 0x00,0x00,0x00}},
    }
};
// black (off)
const script fl_script_black PROGMEM = {
    1, // number of lines
    0, // number of repeats
    {
        { 20, {'c', 0x00,0x00,0x00}},
    }
};

// hue cycle
const script fl_script_hue_cycle PROGMEM = {
    7, // number of lines
    0, // number of repeats
    {
        { 30, {'h', 0x00,0xff,0xff}},  // red
        { 30, {'h', 0x2a,0xff,0xff}},  // yellow 
        { 30, {'h', 0x54,0xff,0xff}},  // green
        { 30, {'h', 0x7e,0xff,0xff}},  // cyan
        { 30, {'h', 0xa8,0xff,0xff}},  // blue
        { 30, {'h', 0xd2,0xff,0xff}},  // magenta
        { 30, {'h', 0xff,0xff,0xff}},  // red
    }
};

// Random color mood light
const script fl_script_randmood PROGMEM = {
    1, // number of lines
    0, // number of repeats
    {
        {50, {'H', 0x80,0x00,0x00}}, // random fade to other hues
    }
};

// virtual candle
const script fl_script_candle PROGMEM = {
    16, // number of lines
    0,  // number of repeats
    {
        {  1, {'f',   10,0x00,0x00}}, // set color_step (fade speed) 
        { 50, {'h', 0x10,0xff,0xff}}, // set orange
        {  2, {'H', 0x00,0x00,0x30}},
        { 27, {'H', 0x00,0x10,0x10}},
        {  2, {'H', 0x00,0x10,0x10}},
        {  7, {'H', 0x00,0x00,0x20}},
        { 10, {'H', 0x00,0x00,0x40}},
        { 10, {'H', 0x00,0x00,0x40}},
        { 10, {'H', 0x00,0x00,0x20}},
        { 50, {'h', 0x0a,0xff,0xff}}, // set orange
        {  1, {'f',   40,0x00,0x00}}, // set color_step (fade speed) 
        {  5, {'H', 0x00,0x00,0xff}},
        {  1, {'H', 0x00,0x00,0x40}},
        {  1, {'H', 0x00,0x00,0x10}},
        {  5, {'H', 0x00,0x00,0x40}},
        {  5, {'H', 0x00,0x00,0x30}},
    }
};

// virtual water
const script fl_script_water PROGMEM = { 
    16, // number of lines
    0,  // number of repeats
    {
        {  1, {'f',   10,0x00,0x00}}, // set color_step (fade speed) 
        { 20, {'h',  140,0xff,0xff}}, // set blue
        {  2, {'H', 0x05,0x00,0x30}},
        {  2, {'H', 0x05,0x00,0x10}},
        {  2, {'H', 0x05,0x00,0x10}},
        {  7, {'H', 0x05,0x00,0x20}},
        { 10, {'H', 0x05,0x00,0x40}},
        { 10, {'H', 0x15,0x00,0x40}},
        { 10, {'H', 0x05,0x00,0x20}},
        { 20, {'h',  160,0xff,0xff}}, // set blue
        {  1, {'f',   20,0x00,0x00}}, // set color_step (fade speed) 
        {  5, {'H', 0x05,0x00,0x40}},
        {  1, {'H', 0x05,0x00,0x40}},
        {  1, {'H', 0x05,0x00,0x10}},
        {  5, {'H', 0x05,0x00,0x20}},
        {  5, {'H', 0x05,0x00,0x30}},
    }
};

// old neon
const script fl_script_oldneon PROGMEM = { 
    16, // number of lines
    0,  // number of repeats
    {
        {  1, {'f',   10,0x00,0x00}}, // set color_step (fade speed) 
        { 20, {'h',   10,0xff,0xff}}, // set reddish orange
        {  2, {'H', 0x05,0x00,0x20}},
        {  2, {'H', 0x05,0x00,0x10}},
        {  2, {'H', 0x05,0x00,0x10}},
        {  7, {'H', 0x05,0x00,0x20}},
        { 10, {'H', 0x05,0x00,0x40}},
        { 10, {'H', 0x15,0x00,0x40}},
        { 10, {'H', 0x05,0x00,0x20}},
        { 20, {'h',   14,0xff,0xff}}, // set reddish orange
        {  1, {'f',   30,0x00,0x00}}, // set color_step (fade speed) 
        {  5, {'H', 0x05,0x00,0xff}},
        {  1, {'H', 0x05,0x00,0x40}},
        {  1, {'H', 0x05,0x00,0x10}},
        {  5, {'H', 0x05,0x00,0x20}},
        {  5, {'H', 0x05,0x00,0x30}},
    }
};

// "the seasons" (cycle)
const script fl_script_seasons PROGMEM = {
    9, // number of lines
    0,  // number of repeats
    {
        {  1, {'f',    4,0x00,0x00}}, // set color_step (fade speed)
        {100, {'h',   70,0xff,0xff}}, // set green/yellow
        { 50, {'H',   10,0x00,0x00}}, // set green/yellow
        {100, {'h',  128,0xff,0xff}}, // set blue/green
        { 50, {'H',   10,0x00,0x00}}, // set blue/green
        {100, {'h',   20,0xff,0xff}}, // set orange/red
        { 50, {'H',   10,0x00,0x00}}, // set orange/red
        {100, {'h',  200,0x40,0xff}}, // set white/blue
        { 50, {'H',   10,0x00,0x00}}, // set white
    }
};

// "thunderstom"  (blues & purples, flashes of white)
const script fl_script_thunderstorm PROGMEM = {
    10, // number of lines
    0,  // number of repeats
    {
        {  1, {'f',    1,0x00,0x00}}, // set color_step (fade speed) 
        {100, {'h',  180,0xff,0x20}}, //
        { 20, {'H',    0,0x10,0x10}}, // randomize a bit
        {100, {'h',  175,0xff,0x20}}, // set dark blueish purple
        {  1, {'f',  200,0x00,0x00}}, // set fast fade speed 
        {  2, {'h',  188,0x00,0xff}}, // white (no saturation)
        {  2, {'h',  178,0x00,0x00}}, // black (no brightness)
        {  4, {'h',  188,0x00,0xff}}, // white (no saturation)
        {  1, {'f',   30,0x00,0x00}}, // 
        { 40, {'h',  172,0xff,0x10}}, // 
    }
};

// stop light
const script fl_script_stoplight PROGMEM = { 
    4, // number of lines
    0,  // number of repeats
    {
        {  1, {'f', 100,0x00,0x00}},  // set color_step (fade speed) 
        {100, {'h',   0,0xff,0xff}},  // set red
        {100, {'h',  90,0xff,0xff}},  // set 'green' (really teal)
        { 30, {'h',  48,0xff,0xff}},  // set yellow
    }
};

// morse code  - SOS
const script fl_script_morseSOS PROGMEM = { 
    17,  // number of lines
    0,  // number of repeats
    {
        { 1,  {'f',   100,0x00,0x00}}, // set color_step (fade speed) 
        { 5,  {'c',  0xff,0xff,0xff}}, 
        { 5,  {'c',  0x00,0x00,0x00}}, 
        { 5,  {'c',  0xff,0xff,0xff}}, 
        { 5,  {'c',  0x00,0x00,0x00}}, 
        { 5,  {'c',  0xff,0xff,0xff}}, 
        {10,  {'c',  0x00,0x00,0x00}}, 
        {10,  {'c',  0xff,0xff,0xff}}, 
        {10,  {'c',  0x00,0x00,0x00}}, 
        {10,  {'c',  0xff,0xff,0xff}}, 
        {10,  {'c',  0x00,0x00,0x00}}, 
        { 5,  {'c',  0xff,0xff,0xff}}, 
        { 5,  {'c',  0x00,0x00,0x00}}, 
        { 5,  {'c',  0xff,0xff,0xff}}, 
        { 5,  {'c',  0x00,0x00,0x00}}, 
        { 5,  {'c',  0xff,0xff,0xff}}, 
        {20,  {'c',  0x00,0x00,0x00}}, 
    }
};


const script* fl_scripts[] PROGMEM = {
    &fl_script_rgb,                    // 1
    &fl_script_blink_white,            // 2
    &fl_script_blink_red,              // 3
    &fl_script_blink_green,            // 4
    &fl_script_blink_blue,             // 5
    &fl_script_blink_cyan,             // 6
    &fl_script_blink_magenta,          // 7
    &fl_script_blink_yellow,           // 8
    &fl_script_black,                  // 9
    &fl_script_hue_cycle,              // 10
    &fl_script_randmood,               // 11
    &fl_script_candle,                 // 12
    &fl_script_water,                  // 13
    &fl_script_oldneon,                // 14
    &fl_script_seasons,                // 15
    &fl_script_thunderstorm,           // 16
    &fl_script_stoplight,              // 17
    &fl_script_morseSOS,               // 18
};


// eeprom begin: muncha buncha eeprom
uint8_t  ee_i2c_addr         EEMEM = I2C_ADDR;
uint8_t  ee_boot_mode        EEMEM = BOOT_PLAY_SCRIPT;
uint8_t  ee_boot_script_id   EEMEM = 0x00;
uint8_t  ee_boot_reps        EEMEM = 0x00;
uint8_t  ee_boot_fadespeed   EEMEM = 0x08;
uint8_t  ee_boot_timeadj     EEMEM = 0x00;
uint8_t  ee_unused2          EEMEM = 0xDA;
script ee_script  EEMEM = {
    6, // number of seq_lines
    0, // number of repeats, also acts as boot repeats?
    {  // dur, cmd,  arg1,arg2,arg3
        {  1, {'f',   10,0x00,0x00}}, // set color_step (fade speed) to 15
        {100, {'c', 0xff,0xff,0xff}},
        { 50, {'c', 0xff,0x00,0x00}},
        { 50, {'c', 0x00,0xff,0x00}},
        { 50, {'c', 0x00,0x00,0xff}},
        { 50, {'c', 0x00,0x00,0x00}}
    }
};
// eeprom end
