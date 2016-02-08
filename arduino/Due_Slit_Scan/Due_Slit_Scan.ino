#include "variant.h"
#include <stdio.h>
#include <adk.h>

// Accessory descriptor. It's how Arduino identifies itself to Android.
char applicationName[] = "SlitScan"; // Slit Scan App
//char applicationName[] = "ArduinoDue"; // bytes counter only, for debug
char accessoryName[] = "Arduino Due"; // your Arduino board
char companyName[] = "hypeastrum";
char versionNumber[] = "0.1";
char serialNumber[] = "1108237412";
char url[] = "http://hypeastrum.org/SlitScan.apk";

USBHost Usb;
ADK adk(&Usb, companyName, applicationName, accessoryName, versionNumber, url, serialNumber);

const unsigned int BUFFER_INDEX_MASK_BITS = 2;
const unsigned int BUFFER_SIZE = 5300;
const unsigned int USB_BUFFER_SIZE = 1024;
const unsigned int BUFFER_COUNT = 1 << BUFFER_INDEX_MASK_BITS;
const unsigned int BUFFER_INDEX_MASK = BUFFER_COUNT - 1;

const unsigned int BUFFER_SIZE_BYTES = (BUFFER_SIZE * 2);

const unsigned int ADC_CHANNEL = 7; // A0
const unsigned int ADC_CHANNEL_MASK = 1<<ADC_CHANNEL;


volatile int adcBufIndex = 0, sendBufIndex = 0;
uint16_t buf[BUFFER_COUNT][BUFFER_SIZE];
uint8_t usbBuf[BUFFER_SIZE];

unsigned long adcReadCounter = 0;
unsigned long adcHandlerCounter = 0;
unsigned long adcIsrCounter = 0;
unsigned long nread;
boolean adkHandshakeDone = false;

uint32_t timestamp;

#define LED_PIN 13
#define CLK_OUT_PIN 22

void setup() {
  Serial.begin(250000);
  cpu_irq_enable();
  
  Serial.println("Init... ");
  pinMode(CLK_OUT_PIN, OUTPUT);
  pinMode(LED_PIN, OUTPUT);

  test();

  initADC();
  Serial.println("Done.");
  //interrupts(); // enable interrupts
}

void test() {
  for (int i = 0; i < 3; i++) {
    digitalWriteDirect(LED_PIN, true);
    delay(500);
    digitalWriteDirect(LED_PIN, false);
    delay(500);
  }
  

  timestamp = micros();
  for (uint32_t i = 0; i < 1000000; i++) {
    digitalWriteDirect(CLK_OUT_PIN, true);
    digitalWriteDirect(CLK_OUT_PIN, false);
  }
  timestamp = micros() - timestamp;
  Serial.print("1M digital hi-lo writes, us: ");
  Serial.println(timestamp);
}

void initADC() {
  pmc_enable_periph_clk(ID_ADC);
  adc_init(ADC, SystemCoreClock, ADC_FREQ_MAX, ADC_STARTUP_FAST);
  
  ADC->ADC_MR |= ADC_CHANNEL_MASK; // free running

  ADC->ADC_CHER = ADC_CHANNEL_MASK; 

  NVIC_EnableIRQ(ADC_IRQn);

  adc_enable_interrupt(ADC, ADC_IER_DRDY); //ADC_IER_DRDY
  
  ADC->ADC_RPR=(uint32_t)buf[adcBufIndex];   // DMA buffer
  ADC->ADC_RCR=BUFFER_SIZE;
  ADC->ADC_RNPR=(uint32_t)buf[adcBufIndex + 1]; // next DMA buffer
  ADC->ADC_RNCR=BUFFER_SIZE;
  
  ADC->ADC_PTCR=1;
  ADC->ADC_CR=2;  
}

void ADC_Handler(){     
  adcIsrCounter++;
  int f = ADC->ADC_ISR;
  if (f & (1<<27)) { 
    adcHandlerCounter++;

    // move DMA pointers to next buffer 
    adcBufIndex = (adcBufIndex + 1) & BUFFER_INDEX_MASK;
    ADC->ADC_RNPR=(uint32_t)buf[adcBufIndex];
    ADC->ADC_RNCR=BUFFER_SIZE;
  } 
}

void loop() {
  Usb.Task();

  boolean isBufferReady = (adcBufIndex != sendBufIndex);

  if (isBufferReady) { // wait for buffer to be full
    adcReadCounter += BUFFER_SIZE;
    Serial.print(adcHandlerCounter);    
    Serial.print("    ");
    Serial.print(adcIsrCounter);    
    Serial.print("    ");
    Serial.print(adcReadCounter);    
    
    if (adk.isReady()) {
      Serial.print("   ADK ready");
      adk.read(&nread, USB_BUFFER_SIZE, usbBuf);    
      if (nread > 0) {
        Serial.print(" RCV: ");
        for (int i = 0; i < nread; i++) {
          Serial.print((char) usbBuf[i]);          
        }
        adk.write(nread, usbBuf);        
        adkHandshakeDone = true;  
      }

      //if (adkHandshakeDone) {
        digitalWriteDirect(LED_PIN, true);
        intToByteArray(buf[sendBufIndex], usbBuf);      
        adk.write(BUFFER_SIZE, usbBuf);          
        digitalWriteDirect(LED_PIN, false);     
        Serial.print("  Sent ");
        Serial.print(BUFFER_SIZE);
        Serial.print(" bytes");
      //}
    } else {
      adkHandshakeDone = false;  
    }

    Serial.println();

    sendBufIndex = (sendBufIndex + 1) & BUFFER_INDEX_MASK;
  }
}

void intToByteArray(uint16_t ints[], uint8_t bytes[]) {
  for (int i = 0; i < BUFFER_SIZE; i++) {
    bytes[i] = ints[i] >> 4;
  }
}

static inline void digitalWriteDirect(int pin, boolean val) {
  if(val) g_APinDescription[pin].pPort -> PIO_SODR = g_APinDescription[pin].ulPin;
  else    g_APinDescription[pin].pPort -> PIO_CODR = g_APinDescription[pin].ulPin;
}
