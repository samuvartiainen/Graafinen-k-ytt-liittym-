// Tunnistetut komennot:
//             
//              L  lämpötila läheteään PC:lle muodossa  +nn.n C
//              K  kellonajan asetus muodossa Khh mm ss
//
// Lämpötilan mittaus NTC-vastuksella
//
// Kytkentä:
//
// a5 ------o------------------------
//          |                       ntc            ntc = tyyppi  10K at +25C
// 0 ----r1-o                        |             r1  = 10K
//                                   |
// gnd-------------------------------
//

#include <stdlib.h>

// Mittausvälin määrittely -------------------
//
// Mittauksen jälkeen mInterval asetetaan arvoon M_INTERVAL
// ja sitä vähennetään 1:llä joka sekunti. Kun mInterval on 0,
// suoritetaan mittaus ja asetetaan mInterval takaisin alkuarvoon
//
const int  CALIBRATION_COEFFICENT = 20; // Lämpötilan korjausarvo
const byte    M_INTERVAL = 3; // mittausväli
unsigned long mseconds;
byte          mInterval = 0;  // mittausvälilaskuri sekunteina

// viimeksi mitattu lämpötilaarvo
// tämä arvo on todellinen arvo x 10, jotta ei tarvita käyttää
// liukulukuja. Esim 234 => lämpötila 23.4C
int temperature = 0; // lämpötila -400.. + 400

// Kellon ajan ylläpito ----------------------
//
// kellon aika on aluksi 00:00:00.
// Sitä kasvatetaan Arduinon millis()- palvelun avulla
// kerran sekunnissa.
// PC-ohjelman on syytä lähettää tarkka aika ainakin
// muutaman kerran päivässä.
byte hour=0, minute=0, second = 0;     // kellon aika

// Päiväyksen ylläpito -----------------------
// 
// aluksi 1.1.2000
//  
byte y=0,m=1,d=1;

// alkuasetukset------------------------------
void setup() {
  Serial.begin(9600);

  mseconds = millis(); // kellonajan ylläpito ilman PC:tä
 }

// sovelluksen ikisilmukka -------------------
void loop() {

   // Tarkistetaan, onko PC:ltä tullut komentoa
   if( Serial.available())
      HandleCommands();
   
   UpdateClock(); //kellonajan päivitys

   // onko aika mitata lämpötila?
   if(mInterval == 0)
   {
     mInterval = M_INTERVAL;  // mittausväli alkuun
     temperature = GetTemperature(); // mittaus
   }
} 

// sovelluksen funktiot -----------------------
//
// HandleCommands:  Tunnistaa kaikki PC:ltä tulevat komennot
// GetTemperature:  Mittaa lämpötilan
// SendTemperature: Lähettää lämpötilan PC:lle
// SendTime:        Lähettää kellonajan PC:lle
//
void HandleCommands()
{
     char command[30] = {0};

        // komennot tavuina, joiden perässä ENTER (\n)
       Serial.readBytesUntil('\n', command, 30);

      //Serial.println(command);

      // kellon ajan asetus ----------------------
      if(command[0] == 'K') // komennon muoto Chh mm ss
      {
        hour = atoi(&command[1]);
        minute = atoi(&command[4]);
        second = atoi(&command[7]);
      }
      else if(command[0] == 'L') // lämpötilan luku
      {
        SendTemperature(); // lähetetään lämpötila
      }
}

int GetTemperature() // mittaa lämpötilan ntc-vastuksella 
{
   unsigned rawValue;            // 0..1023
   unsigned long mes_resistance; //  ohmia mitattu resistanssi
   long temperature;             // -40..+40 C * 10 = -400..+400 (1.desimaali mukana kokonaisosassa)

   pinMode(0,OUTPUT);   
   digitalWrite(0,HIGH);     // Vout päälle mittauksen ajaksi
   rawValue = analogRead(5); // analogiatulon luenta
   digitalWrite(0,LOW);      // Vout pois mittauksen jälkeen

   // lasketaan ensin resistanssi 
   mes_resistance = (10000L * rawValue) / ( 1023-rawValue); // ensin kertolaskut, jakolasku lopuksi, koska lasketaan kokonaisluvuilla

  // --------------------------------------------------------------------------------------------------------
  // ND06P00103J - tyypin resistanssi alueella -40..+40 5C asteen portaina
  //
  // 407200,289500,208000,151000,110700,81970,61230,46150,35080,26880,20760,16160,12670,10000,7949,6359,5120 ohm
  //  -40    -35    -30    -25     -20   -15   -10   -5     0    +5    +10  +15   +20   +25   +30   +35 +40  C
  // --------------------------------------------------------------------------------------------------------
  long ntc_resistances[] = {407200,289500,208000,151000,110700,81970,61230,46150,35080,26880,20760,16160,12670,10000,7949,6359,5120};

  // onko alueella eli osuuko yllä olevaan taulukkoon?
  if((mes_resistance < ntc_resistances[16]) || (mes_resistance > ntc_resistances[0]))
     temperature = 999; // ei tuetulla alueella
 
  temperature = -400; // kylmin tuettu arvo -40C

  // mennään taulukkoa läpi, kunnes löydetään väli, johon mitattu resistanssi kuuluu
  int i = 0;
  do
  {
    if(mes_resistance < ntc_resistances[i+1])
    {
      temperature += 50; // +5C tasaväli
      i++;
    }
    else
    {
      // mitä tasaluvun päälle? (0..5C)
      temperature +=((50L * (ntc_resistances[i]-mes_resistance))/(ntc_resistances[i]-ntc_resistances[i+1]) ); // 00..49
      break; 
    }
  }
  while (i < 16);

  return (int)temperature - CALIBRATION_COEFFICENT; // tässä voi lisätä/vähentää vakion, jos mittausarvo poikkeaa esim. +-0.5C todellisesta
}

void SendTemperature() // 10 x arvo jaettava
{
  Serial.print(' ');
  Serial.print(temperature/10);      // täydet asteet
  Serial.print('.');                 // desimaalipiste
  Serial.print(abs(temperature%10)); // desimaaliosa 0..9
  Serial.println(" C");
}
void UpdateClock()
{
  unsigned long now = millis();
  if (now >= (mseconds + 1000)) // kellon aika kulunut 1s, jos millisekunnit kasvaneet 1000:lla
  {
    second++;
    mseconds = now;
    if (second >= 60)
    {
      minute++;
      second=0;
      if(minute >= 60)
      {
        minute = 0;
        hour++;
        if(hour >= 24)
          hour = 0;
          //UpdateCalendar(); // new day begins
      }
    }
    mInterval--; // mittausvälin sekunneista 1 pois
  }
}

void UpdateCalendar() //updates calendar 
{
  unsigned char monthLen[] = {0,31,28,31,30,31,30,31,31,30,31,30,31};
  unsigned char  leapDay;

  if ( (y % 4 == 0) && ( m==2))
  leapDay = 1;
  else
  leapDay = 0;

  if(d < ( (monthLen[m]) + leapDay))
  {
    d++;
    return;
  }
  else
  {
    if (m < 12)
    {
      m++;
      d=1;
      return;
    }
    else
    { 
      y++;
      d=1;
      m=1;
    }
  }
}


// lähetetään kellonaika 
void SendTime()  // muoto hh:mm:ss
{
  if(hour < 10) // jos alle 10, lisätään etunolla
     Serial.print('0');
  Serial.print(hour);
  Serial.print(':');
  
  if(minute < 10) // jos alle 10, lisätään etunolla
    Serial.print('0');
  Serial.print(minute); 
  Serial.print(':');
  
  if(second < 10) // jos alle 10, lisätään etunolla
    Serial.print('0');
  Serial.print(second); 
  Serial.println();
}
