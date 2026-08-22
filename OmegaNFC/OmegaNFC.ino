#include <Wire.h>
#include <SPI.h>
#include <Adafruit_PN532.h>
#include <stdint.h>
#include <stdbool.h>
#if defined(ARDUINO_ARCH_SAMD)
#define Serial SerialUSB
#endif
#define PN532_SCK  (14) //D5
#define PN532_MOSI (13) //D7
#define PN532_SS   (4)  //D2
#define PN532_MISO (12) //d6
#define PN532_IRQ   (2)
#define PN532_RESET (3)  // Not connected by default on the NFC Shield
Adafruit_PN532 nfc(PN532_SCK, PN532_MISO, PN532_MOSI, PN532_SS);


uint8_t datacredit1[16];
uint8_t datacredit2[16];
uint8_t datacont[16];
uint8_t keyb[6];


//###############################################################################################################################################################################################
//#                 #################################################################          V O I D - S E T U P       #######################################################                #
//###############################################################################################################################################################################################
void setup(void) {
                  delay(1000);
                  #ifndef ESP8266
                  while (!Serial); // for Leonardo/Micro/Zero
                  #endif
                  Serial.begin(115200);    
                  Serial.println("");
                  Serial.println("");
                  Serial.println("####################################################");  
                  Serial.println("#####      OMEGA NFC v1.4 Scrittura            #####");
                  Serial.println("#####          Last upgrade          9         #####");
                  Serial.println("####################################################"); 
                  pinMode(LED_BUILTIN, OUTPUT);
                  nfc.begin();
                  delay(500);
                  uint32_t versiondata = nfc.getFirmwareVersion();
                  if (! versiondata) {
                                      Serial.print("Lettore non rilevato");
                                      while (1); // halt
                                     }               
                  Serial.print("Rilevato Chip NFC Reader PN5"); Serial.println((versiondata >> 24) & 0xFF, HEX);
                  Serial.print("Firmware NFC Reader ver. "); Serial.print((versiondata >> 16) & 0xFF, DEC);
                  Serial.print('.'); Serial.println((versiondata >> 8) & 0xFF, DEC);
                  nfc.setPassiveActivationRetries(0xFF);
                  nfc.SAMConfig();              
                  Serial.println("############################# ATTESA CHIAVE #################################");
                }
//###############################################################################################################################################################################################
//###############################################################################################################################################################################################


                
                
//###############################################################################################################################################################################################
//#                 #############################################################         V O I D - L O O P       ##############################################################                #
//###############################################################################################################################################################################################




void loop(void) {
  
                  boolean success;
                  uint8_t autenticazione_ok;
                  uint8_t scrittura_ok;
                  uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer to store the returned UID
                  uint8_t uidLength;        // Length of the UID (4 or 7 bytes depending on ISO14443A card type)
                  success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, &uid[0], &uidLength);

                  if (success) {
                                Serial.println("# Chiave rilevata!                                                          #");
                                Serial.print("# Lunghezza UID : ");
                                Serial.print(uidLength, DEC);
                                Serial.println(" bytes                                                   #");
                                Serial.print("# Valore UID: ");
                                char totUid;
                                String convUid;
                                for (uint8_t i = 0; i < uidLength; i++)
                                    {
                                      // Serial.print(" 0x");
                                      Serial.print(uid[i], HEX);
                                      totUid = (uid[i]);
                                      convUid += String(totUid, HEX); // VAR CON UID CONVERTITO
                                    }
                                Serial.print("                                                      #");
                                delay(2000);
                                Serial.println("");


//######################################################################


                              if (uidLength == 4) {
                                    autenticazione_ok = 0;
                                    //########################## START -  ################################      
                                    if (convUid == "93addb69") {
                                                                 Serial.println("# UID Valido!                                                               #");
                                                                 Serial.println("###########                Chiave Gv !                  ###########");
                                                                 memcpy(keyb, (const uint8_t[]) {0xA8, 0x8E, 0x0A, 0x53, 0xE9, 0x28}, sizeof keyb); //chiave b del settore 
                                                                 autenticazione_ok = nfc.mifareclassic_AuthenticateBlock(uid, uidLength, 9, 2, keyb);
                                                                 Serial.println("# Scrittura Settore 2 KeyB: A88E0A5                                    #");                                                                            
                                                                 if (autenticazione_ok) {
                                                                                          Serial.println("# Auth OK                                                                   #");                                                                                       
                                                                                          //########################## Salva dati in memoria ################################
                                                                                          Serial.println("# Scrittura -- 17.45                                                   #"); 
                                                                                          memcpy(datacredit1, (const uint8_t[]) {0x00, 0xC0, 0x06, 0xC6, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xA0  }, sizeof datacredit1);    //blocco da scrivere (-- new)
                                                                                          memcpy(datacredit2, (const uint8_t[]) {0x00, 0xD1, 0x06, 0xD7, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xA1  }, sizeof datacredit2);    //blocco da scrivere (-- new)
                                                                                          memcpy(datacont, (const uint8_t[]) {0xAA, 0xA1, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00  }, sizeof datacont);    //blocco da scrivere (contatore)
                              
                                                                                          //########################## Scrivi su chiave ######################################
                                                                                          Serial.println("# Scrittura -- OLD OK                                                  #");
                                                                                          scrittura_ok = nfc.mifareclassic_WriteDataBlock (8, datacredit1);
                                                                                          Serial.println("# Scrittura -- NEW OK                                                  #");
                                                                                          scrittura_ok = nfc.mifareclassic_WriteDataBlock (9, datacredit2);
                                                                                          Serial.println("# Scrittura Contatore OK                                                    #");
                                                                                          scrittura_ok = nfc.mifareclassic_WriteDataBlock (10, datacont);
                                                                                          //######################### LAMPEGGIO ##############################
                                                                                          wrledok(); // richiamo funzione
                                                                                          }   
                                                                 else {wledko();}
                                                                }     
                                  //########################## END -   ################################ 








                                  //########################## START  ################################ 
                                  else if (convUid == "3e69a045") {
                                                                  Serial.println("# UID Valido!                                                               #");
                                                                  Serial.println("###########                   Chiave                           ###########");
                                                                  memcpy(keyb, (const uint8_t[]) {0xEB, 0xEF, 0xFD, 0x86, 0x91, 0xB9}, sizeof keyb); //chiave b del settore --
                                                                  autenticazione_ok = nfc.mifareclassic_AuthenticateBlock(uid, uidLength, 6, 1, keyb);
                                                                  Serial.println("# Scrittura Settore 1 KeyB: EBEFFD869                                    #");
                                                                  if (autenticazione_ok) {
                                                                                           Serial.println("# Auth OK                                                                   #");
                                                                                           //########################## Salva dati in memoria ################################
                                                                                           Serial.println("# Scrittura -- 10.00                                                   #"); 
                                                                                           memcpy(datacredit1, (const uint8_t[]) {0x00, 0x03, 0xE8, 0x01, 0x07, 0xD9, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00  }, sizeof datacredit1);    //blocco da scrivere (--)
                                                                                           Serial.println("# Scrittura --  OK                                                  #");
                                                                                           scrittura_ok = nfc.mifareclassic_WriteDataBlock (6, datacredit1);
                                                                                           //######################### LAMPEGGIO ##############################
                                                                                           wrledok();
                                                                                          }
                                                                 else {wledko();}
                                                                 }
                                  //########################### END  ##################################
                                             
     







                                  //########################## START  ################################ 
                                  else if (convUid == "e2118959" || convUid =="d4cb2d39" || convUid =="6a7f1ed2") {           
                                                                  Serial.println("###########                   MEDIOLANUM                        ###########");
                                                                  memcpy(keyb, (const uint8_t[]) {0xAB, 0xD4, 0x55, 0x7B, 0xB1, 0xAA}, sizeof keyb); //chiave b del settore --
                                                                  autenticazione_ok = nfc.mifareclassic_AuthenticateBlock(uid, uidLength, 9, 2, keyb);
                                                                  Serial.println("# Scrittura Settore 2 KeyB: ABD4557BB1AA                            #");
                                                                  if (autenticazione_ok) {                                             
                                                                                          Serial.println("# Auth OK                                                                   #");                                     
                                                                                          //########################## Salva dati in memoria ################################
                                                                                          Serial.println("# Scrittura  10.00                                                   #"); 
                                                                                          memcpy(datacredit1, (const uint8_t[]) {0x03, 0xE8, 0x00, 0x1E, 0x7E, 0xE3, 0x56, 0xCF, 0x00, 0x2E, 0x2B, 0x1D, 0xFF, 0xFF, 0x04, 0x4C  }, sizeof datacredit1);    //blocco da scrivere (--)
                                                                                          if (convUid == "e2118959" ){ Serial.println("# Scrittura  Gv  OK                                              #");    }
                                                                                          if (convUid == "d4cb2d39" ){ Serial.println("# Scrittura  Nik  OK                                             #");    }    
                                                                                          if (convUid == "6a7f1ed2" ){ Serial.println("# Scrittura  Andre  OK                                           #");    } 
                                                                                          else { Serial.println("# Chiave non censita                                                        #");  }                                                                                                                                     
                                                                                          scrittura_ok = nfc.mifareclassic_WriteDataBlock (8, datacredit1);   
                                                                                          //######################### LAMPEGGIO ##############################
                                                                                          wrledok();
                                                                                          }
                                                                  else {wledko();}
                                                                  }
                                  //########################## END  ################################ 03E8001E7EE356CF002E2B1DFFFF044C



                                  
                                  //########################## START Lava ################################ 
                                  else if (convUid == "4bbf6a4b") {           
                                                                 Serial.println("# UID Valido!                                                               #");
                                                                 Serial.println("###########             Chiave L!              ###########");                                                         
                                                                 memcpy(keyb, (const uint8_t[]) {0x9E, 0x62, 0x75, 0x7E, 0x6F, 0xD3}, sizeof keyb); //chiave b del settore 
                                                                 autenticazione_ok = nfc.mifareclassic_AuthenticateBlock(uid, uidLength, 12, 3, keyb);
                                                                 Serial.println("# Scrittura Settore 2 KeyB: 9E62757E                                   #");                                                                            
                                                                 if (autenticazione_ok) {
                                                                                          Serial.println("# Auth OK                                                                   #");
                                                                                                                                                                             
                                                                                          //########################## Salva dati in memoria ################################
                                                                                          Serial.println("# Scrittura  49.00                                                   #"); 
                                                                                          memcpy(datacredit1, (const uint8_t[]) {0x24, 0x13, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F, 0x77  }, sizeof datacredit1);    //blocco 1 settore 3 da scrivere
                                                                                          memcpy(datacredit2, (const uint8_t[]) {0xC0, 0x12, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF9, 0x12  }, sizeof datacredit2);    //blocco 2 settore 3 da scrivere
                                                                                          memcpy(datacont, (const uint8_t[]) {0x7F, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF  }, sizeof datacont);    //blocco 3 settore 3 da scrivere
                              
                                                                                          //########################## Scrivi su chiave ######################################
                                                                                          Serial.println("# Scrittura  OLD OK                                                  #");
                                                                                          scrittura_ok = nfc.mifareclassic_WriteDataBlock (12, datacredit1);
                                                                                          Serial.println("# Scrittura  NEW OK                                                  #");
                                                                                          scrittura_ok = nfc.mifareclassic_WriteDataBlock (13, datacredit2);
                                                                                          Serial.println("# Scrittura Contatore OK                                                    #");
                                                                                          scrittura_ok = nfc.mifareclassic_WriteDataBlock (14, datacont);
                                                                                          //######################### LAMPEGGIO ##############################
                                                                                          wrledok(); // richiamo funzione
                                                                                          } 
                                                                   else {wledko();}
                                                                  }
                                  //########################## END Lav ################################


                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       


                                                                    //########################## START La ################################ 
                                  else if (convUid == "11575edb") {           
                                                                 Serial.println("# UID Valido!                                                               #");
                                                                 Serial.println("###########             Chiave                                    ###########");                                                         
                                                                 memcpy(keyb, (const uint8_t[]) {0x1B, 0x8B, 0x49, 0x1A, 0xBB, 0x3A}, sizeof keyb); //chiave b del settore 
                                                                 autenticazione_ok = nfc.mifareclassic_AuthenticateBlock(uid, uidLength, 12, 3, keyb);
                                                                 Serial.println("# Scrittura Settore 2 KeyB: 1B8B491ABB3A                                    #");                                                                            
                                                                 if (autenticazione_ok) {
                                                                                          Serial.println("# Auth OK                                                                   #");
                                                                                                                                                                             
                                                                                          //########################## Salva dati in memoria ################################
                                                                                          Serial.println("# Scrittura  49.00                                                   #"); 
                                                                                          memcpy(datacredit1, (const uint8_t[]) {0x24, 0x13, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F, 0x77  }, sizeof datacredit1);    //blocco 1 settore 3 da scrivere
                                                                                          memcpy(datacredit2, (const uint8_t[]) {0xC0, 0x12, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF9, 0x12  }, sizeof datacredit2);    //blocco 2 settore 3 da scrivere
                                                                                          memcpy(datacont, (const uint8_t[]) {0x7F, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF  }, sizeof datacont);    //blocco 3 settore 3 da scrivere
                              
                                                                                          //########################## Scrivi su chiave ######################################
                                                                                          Serial.println("# Scrittura  OLD OK                                                  #");
                                                                                          scrittura_ok = nfc.mifareclassic_WriteDataBlock (12, datacredit1);
                                                                                          Serial.println("# Scrittura  NEW OK                                                  #");
                                                                                          scrittura_ok = nfc.mifareclassic_WriteDataBlock (13, datacredit2);
                                                                                          Serial.println("# Scrittura Contatore OK                                                    #");
                                                                                          scrittura_ok = nfc.mifareclassic_WriteDataBlock (14, datacont);
                                                                                          //######################### LAMPEGGIO ##############################
                                                                                          wrledok(); // richiamo funzione
                                                                                          } 
                                                                   else {wledko();}
                                                                  }
                                  //########################## END  Va ################################

      
 
      
                                  else
                                      {
                                      wledko();
                                      }


                               }
                       }
            } //loop

//###############################################################################################################################################################################################
//###############################################################################################################################################################################################




//###############################################################################################################################################################################################
////#                 ######################################################################  F U N Z I O N I ##################################################################                #
//###############################################################################################################################################################################################

// #### Scrittura OK 
void wrledok () {      
                  for (int i = 0; i <= 3; i++) {
                                                digitalWrite(LED_BUILTIN, HIGH);                         //tre lampeggi veloci avvenuta scrittura
                                                delay(100);
                                                digitalWrite(LED_BUILTIN, LOW);
                                                delay(100);
                                               }
                  Serial.println("# Scrittura Eseguita .. attesa 5 sec. prima della prossima scrittura!       #");          
                  wait5();
                 }





  
// #### Scrittura KO
void wledko () {     
                Serial.println("# Riconoscimeto o Autenticazione fallita!                                   #");
                for (int i = 0; i <= 4; i++) {
                                              digitalWrite(LED_BUILTIN, HIGH);                         //4 lampeggi lenti scrittura fallita
                                              delay(500);
                                              digitalWrite(LED_BUILTIN, LOW);
                                              delay(500);
                                             }
                 wait5();
              }





// #### ATTESA 5sec
void wait5 () {
               Serial.print("# WAIT:");
               for (int i = 5; i > 0; i--) {
                                             Serial.print(" ");     
                                             Serial.print(i);               
                                             delay(1000);
                                            }
               Serial.print(" 0 !                                                       #");
               delay(1000);
               Serial.println("");
               Serial.println("#############################################################################");
               Serial.println("");
               Serial.println("############################# ATTESA CHIAVE #################################");
               Serial.println("#############################################################################");
               }
               
//###############################################################################################################################################################################################
//###############################################################################################################################################################################################