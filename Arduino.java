//  Java-ohjelmointi  2018                  Graafinen käyttöliittymä - Samu Vartiainen
//  
//  
//
package arduino;
import org.jfree.data.xy.XYSeries;
import java.time.LocalDateTime;
import java.awt.Color;
import javax.swing.*;
import java.awt.Graphics;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.ValueAxis;
import static org.jfree.chart.plot.PlotOrientation.VERTICAL;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
@SuppressWarnings("OverridableMethodCallInConstructor")

public class Arduino extends JFrame {

    static final String PORT = "COM3";
    static final int SPEED = 9600;
    static final int BUFFER_SIZE = 20;
    private final long createdMillis = System.currentTimeMillis();

    Display p;
    JButton update = new JButton("Päivitä lämpötila");
    JRadioButton line = new JRadioButton("Viiva");
    JRadioButton bar = new JRadioButton("Pylväs");
    JButton rbday = new JButton("Valitse päivä");
    JButton rbnow = new JButton("Nyt");

    Color color = Color.GREEN;
    Timer timer;
    Timer timer2;
    XYSeries series = new XYSeries("Lämpötila");
    TimeSeries series1 = new TimeSeries("Lämpötila", "Domain", "Range", Minute.class);
    final ButtonGroup chartbuttons = new ButtonGroup();
    final ButtonGroup timebuttons = new ButtonGroup();

    static String value = "0";
    String fileName;
    double[] temperatures = new double[480];
    ArrayList<Double> TempList = new ArrayList<Double>();
    // static DecimalFormat df = new DecimalFormat(".##"); 
    Date d;
    int HowManyCharts1 = 0; // muuttuja montako diagrammia käyttäjä on avannut
    int HowManyCharts2 = 0; 

    public Arduino() {
        // tehdään paneeli
        p = new Display();

        update.setLayout(null);
        update.setBounds(150, 20, 130, 30);

        p.add(update, BorderLayout.PAGE_START); // lisätään painonappi

        //RADIOBUTTONIT
        bar.setLayout(null);
        bar.setBounds(300, 20, 100, 30);

        p.add(bar, BorderLayout.PAGE_START);

        line.setLayout(null);
        line.setBounds(410, 20, 100, 30);

        p.add(line, BorderLayout.PAGE_START);

        rbnow.setLayout(null);
        rbnow.setBounds(540, 20, 90, 30);

        p.add(rbnow, BorderLayout.PAGE_START);

        rbday.setLayout(null);
        rbday.setBounds(640, 20, 120, 30);

        p.add(rbday, BorderLayout.PAGE_START);
        p.setLayout(new BorderLayout());
        add(p); // paneeli lisätään ohjelmaan

        line.setActionCommand("line");
        bar.setActionCommand("bar");
        chartbuttons.add(bar);
        chartbuttons.add(line);
        line.setSelected(true);
//bar.setSelected(false);
//timebuttons.add(rbday);
//timebuttons.add(rbnow);
//rbnow.setSelected(false);
        p.setVisible(true);

        // ACTIONLISTENERIT RADIOBUTTONEILLE
        rbnow.addActionListener(
                (ActionEvent event) -> {
                    if (bar.isSelected()) {
                        barButton();
                        FirstTempWhenNowPressed();
                        HowManyCharts1++;
                        HowManyCharts2++;
                    } else if (line.isSelected()) {
                        lineButton();
                        FirstTempWhenNowPressed();
                        HowManyCharts1++;
                        HowManyCharts2++;
                    }

                });

        rbday.addActionListener(
                (ActionEvent event) -> {
                    if (bar.isSelected()) {
                        rbdayBarChart();
                        HowManyCharts1++;
                    } else if (line.isSelected()) {
                        rbdayLineChart();
                        HowManyCharts1++;
                    }

                });

        // tämä koodi suoritetaan, kun painonappia käytetään
        update.addActionListener(
                (ActionEvent event) -> {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;

                    Serial.SendLineToComPort("L\n"); // pyydetään Arduinolta lämpötila
                    try {
                        Thread.sleep(100L); // hieman vastausaikaa eli 100ms
                    } catch (InterruptedException ex) {
                    }

                    if ((len = Serial.GetLineFromComPort(buffer, BUFFER_SIZE)) > 0) {
                        value = BytesToString(buffer, len); // näytetään, mitä Arduino vastaa
                    }
                    p.repaint(); // maalataan paneeli uudestaan
                }
        );

        // laitetaan ikkunan ominaisuudet
        setTitle("Harjoitustyö"); // otsikko
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);// voidaan pysäyttää
        setSize(new Dimension(800, 550)); // laitetaan kiinteä koko
        setResizable(true); // kokoa ei voi muuttaa

        setVisible(true);    // ikkuna näkyviin

        UseTimer(); // mitataan säännöllisin välein   
        update.doClick();

    }

    public void lineButton() {

        setVisible(false);

        XYDataset dataset1 = new TimeSeriesCollection(series1);
        JFreeChart chart = ChartFactory.createTimeSeriesChart("Linechart", "Kellonaika", "Lämpötila (Celsius)", dataset1, true, true, false);
        XYPlot plot = (XYPlot) chart.getPlot();
        DateAxis xAxis = (DateAxis) plot.getDomainAxis();
        xAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
        xAxis.setAutoRange(true);

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseShapesVisible(true);
        ValueAxis yAxis = plot.getRangeAxis();    //y-akseli
        yAxis.setLowerBound(-40.0);               //y-akselin alaraja
        yAxis.setUpperBound(40.0);                //y-akselin yläraja;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        if (HowManyCharts1 == 0) {

            p.setLayout(new BorderLayout()); //chartpanelille uusi layout
            p.add(new ChartPanel(chart), BorderLayout.PAGE_END);

            setVisible(true);
        } else {
            p.setLayout(new BorderLayout()); //chartpanelille uusi layout
            p.add(new ChartPanel(chart), BorderLayout.PAGE_END);
            setVisible(true);
            JFrame frame = new JFrame(); // uusi ikkuna toiselle chartille
            frame.setSize(new Dimension(850, 480));
            frame.setLayout(new BorderLayout());
            frame.add(new ChartPanel(chart), BorderLayout.PAGE_END);
            frame.setVisible(true);
        }
    }

    public void barButton() {

        setVisible(false);

        IntervalXYDataset dataset2 = new TimeSeriesCollection(series1);
        JFreeChart chart = ChartFactory.createXYBarChart("Barchart", "Kellonaika", true, "Lämpötila (Celsius)", dataset2, VERTICAL, true, true, false);
        XYPlot plot = (XYPlot) chart.getPlot();

        DateAxis xAxis = (DateAxis) plot.getDomainAxis();

        xAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
        xAxis.setAutoRange(true);
     
        xAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);

        ValueAxis yAxis = plot.getRangeAxis();
        yAxis.setLowerBound(-40.0);
        yAxis.setUpperBound(40.0);

        if (HowManyCharts1 == 0) {

            p.setLayout(new BorderLayout()); //chartpanelille uusi layout
            p.add(new ChartPanel(chart), BorderLayout.PAGE_END);

            setVisible(true);
        } else {
            p.setLayout(new BorderLayout()); //chartpanelille uusi layout
            p.add(new ChartPanel(chart), BorderLayout.PAGE_END);
            setVisible(true);
            JFrame frame = new JFrame(); // uusi ikkuna toiselle chartille
            frame.setSize(new Dimension(850, 480));
            frame.setLayout(new BorderLayout());
            frame.add(new ChartPanel(chart), BorderLayout.PAGE_END);
            frame.setVisible(true);
        }
    }

    public void rbdayLineChart() {
        setVisible(false);
        fileName = promptForFile(); // Funktiossa avataan tiedosto koneelta, jossa lämpötiladataa joltakin päivältä
        XYSeries series2 = new XYSeries("Lämpötila");
        IntervalXYDataset dataset2 = new XYSeriesCollection(series2);
        JFreeChart chart2 = ChartFactory.createXYLineChart(fileName, "Kellonaika", "Lämpötila (Celsius)", dataset2, VERTICAL, true, true, false);

        XYPlot plot = (XYPlot) chart2.getPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseShapesVisible(true);

        ValueAxis xAxis = plot.getDomainAxis();
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(24);
        ValueAxis yAxis = plot.getRangeAxis();
        yAxis.setLowerBound(-40.0);
        yAxis.setUpperBound(40.0);
     
    
        if (fileName != null) {   

            try (Scanner editoitavaTiedosto = new Scanner(new File(fileName))) {
              
                while ((editoitavaTiedosto.hasNextLine())) {
                    
                    double Time = Double.parseDouble(editoitavaTiedosto.nextLine()); // valitun tiedoston joka toinen rivi luetaan kellonajaksi ja lämpötilaksi
                    double X = Double.parseDouble(editoitavaTiedosto.nextLine());
                   
                    series2.add(Time, X);  // lisätään datasarjaan kellonaika ja lämpötila
                }
                editoitavaTiedosto.close();
            } catch (FileNotFoundException ex) {
                // ei pitäisi tapahtua
            }
        }

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        if (HowManyCharts1 == 0) {

            p.setLayout(new BorderLayout()); //chartpanelille uusi layout
            p.add(new ChartPanel(chart2), BorderLayout.PAGE_END);

            setVisible(true);
        } else {
            p.setLayout(new BorderLayout()); //chartpanelille uusi layout
            p.add(new ChartPanel(chart2), BorderLayout.PAGE_END);
            setVisible(true);
            JFrame frame = new JFrame(); // uusi ikkuna toiselle chartille
            frame.setSize(new Dimension(850, 480));
            frame.setLayout(new BorderLayout());
            frame.add(new ChartPanel(chart2), BorderLayout.PAGE_END);
            frame.setVisible(true);
        }
    }

    public void rbdayBarChart() {
        setVisible(false);
        fileName = promptForFile(); // kysytään avattavan tiedoston nimi
        // Avataan tiedosto koneelta, jossa lämpötiladataa joltakin päivältä
        XYSeries series2 = new XYSeries("Lämpötila");
        IntervalXYDataset dataset2 = new XYSeriesCollection(series2); 

        JFreeChart chart2 = ChartFactory.createXYBarChart(fileName, "Kellonaika", false, "Lämpötila (Celsius)", dataset2, VERTICAL, true, true, false);
        XYPlot plot = (XYPlot) chart2.getPlot();

        ValueAxis xAxis = plot.getDomainAxis();

        xAxis.setLowerBound(-0.6); // jotta palkit näkyy kokonaan
        xAxis.setUpperBound(24.6);

        
        ValueAxis yAxis = plot.getRangeAxis();
        yAxis.setLowerBound(-40.0);
        yAxis.setUpperBound(40.0);
       
  

      
        if (fileName != null) {   

            try (Scanner editoitavaTiedosto = new Scanner(new File(fileName))) {
          
                while ((editoitavaTiedosto.hasNextLine())) {
                 
                    double Time = Double.parseDouble(editoitavaTiedosto.nextLine()); // valitun tiedoston joka toinen rivi luetaan kellonajaksi ja lämpötilaksi
                    double X = Double.parseDouble(editoitavaTiedosto.nextLine());

                    series2.add(Time, X); // lisätään datasarjaan kellonaika ja lämpötila

                }
                editoitavaTiedosto.close();
            } catch (FileNotFoundException ex) {
                // ei pitäisi tapahtua
            }
        }

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        if (HowManyCharts1 == 0) {

            p.setLayout(new BorderLayout()); //chartpanelille uusi layout
            p.add(new ChartPanel(chart2), BorderLayout.PAGE_END);

            setVisible(true);
        } else {
            p.setLayout(new BorderLayout()); //chartpanelille uusi layout
            p.add(new ChartPanel(chart2), BorderLayout.PAGE_END);
            setVisible(true);
            JFrame frame = new JFrame(); // uusi ikkuna toiselle chartille
            frame.setSize(new Dimension(850, 480));
            frame.setLayout(new BorderLayout());
            frame.add(new ChartPanel(chart2), BorderLayout.PAGE_END);
            frame.setVisible(true);
        }
    }

    public static void infoBox(String infoMessage, String titleBar) {
        JOptionPane.showMessageDialog(null, infoMessage, "Info " + titleBar, JOptionPane.INFORMATION_MESSAGE);
    }

    private String promptForFile() { // kysytään avattavan tiedoston nimi

        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("c:\\temp"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Vain tekstitiedostot", "txt");
        fc.setFileFilter(filter);

        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile().getAbsolutePath();
        } else {
            return null;
        }

    }

    public int TimeInSecondsForChart() {

        long nowMillis = System.currentTimeMillis();

        return (int) ((nowMillis - this.createdMillis) / 1000);
    }

    public void MakeFile() throws FileNotFoundException, IOException {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        BufferedReader reader = null;
        File file = new File(dateFormat.format(date) + ".txt"); // Tiedoston nimeksi annetaan tämän päivän päivämäärä .txt

        if (file.exists()) {
            reader = new BufferedReader(new FileReader(file));
            java.util.List<Double> list = new ArrayList<Double>();
            String text = null;

            while ((text = reader.readLine()) != null) {
                //  text = reader.readLine(); // luetaan uudestaan sillä tiedostossa joka toinen arvo on kellonaika ja lämpötila (kellonaika -> uusi rivi -> lämpötila)
                //  list.add(Double.parseDouble(text));
            }
           
            BufferedWriter eFileWrite = new BufferedWriter(new FileWriter(file, true)); // true parametri ettei kirjoita päälle
            eFileWrite.write("\n");
            eFileWrite.write(Integer.toString(HourForChart() - 1));
            eFileWrite.write("\n");
            eFileWrite.write(Double.toString(HourAverageTemp()));
            eFileWrite.close();
        } else {
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(Integer.toString(HourForChart() - 1));
            out.write("\n");
            out.write(Double.toString(HourAverageTemp()));
            out.close();
        }
    }

    public void UseTimer() {

        timer = new Timer(180000, (ActionEvent evt) -> {             // kysely 3 sekunnin välein
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            int secondCounter = 0;

            Serial.SendLineToComPort("L\n"); // pyydetään Arduinolta lämpötila
            try {
                Thread.sleep(100L); // hieman vastausaikaa eli 100ms

            } catch (InterruptedException ex) {
            }

            if ((len = Serial.GetLineFromComPort(buffer, BUFFER_SIZE)) > 0) {
                value = BytesToString(buffer, len); // näytetään, mitä Arduino vastaa
            }
            String valueParse = value.replaceAll("[^0-9]", "");
            double valueDouble = Double.parseDouble(valueParse) / 10;
            String minutes = (MinuteForChart() < 10 ? "0" : "") + MinuteForChart(); // minuutit muodossa 01
            Date date = new Date();
           
            Minute currentTime = new Minute(); // HH:mm format tapahtuu bar/linebutton funktioissa

            series1.addOrUpdate(currentTime, valueDouble); // lisätään sarjaan
       

            // Tunnin vaihtumisen ehto, jos minuutit yli 2, lasketaan viime tunnin lämpötilojen keskiarvo TempList:stä ja kirjoitetaan tunti ja ka tiedostoon
            if (MinuteForChart() > 2) {

                // Lisätään templistiin lämpötila
                TempList.add(valueDouble);

                // Tulostetaan TempList sekä aika
                // String minutes = (MinuteForChart() < 10 ? "0" : "") + MinuteForChart();
                System.out.println("Klo " + HourForChart() + ":" + minutes + "\t" + TempList);
                // System.out.println(TempList);

            } else if (MinuteForChart() <= 2) {
                try {
                    // kutsutaan funktiota joka laskee edellisen tunnin lämpötilojen keskiarvon (TempList) ja kirjoittaa tiedostoon tunnin sekä ka lämpötilan
                    MakeFile();
                    System.out.println("Viime tunnin lämpötilojen keskiarvo " + HourAverageTemp() + " lisätty tiedostoon!");
                    System.out.println("Viime tunnin korkein lämpötila oli " + HighestTemp() + " ja alhaisin lämpötila oli " + LowestTemp() + " astetta.");
                } catch (IOException ex) {
                    Logger.getLogger(Arduino.class.getName()).log(Level.SEVERE, null, ex);
                }
                // Tyhjennetään TempList, seuraavan tunnin lämpötiloja varten
                TempList.clear();
                System.out.println("TempList tyhjennetty seuraavan tunnin lämpötiloja varten");
                TempList.add(valueDouble); // Uuteen listaan ensimmäinen arvo
                System.out.println("Klo " + HourForChart() + "." + minutes + "\t" + TempList);
            }

            p.repaint(); // maalataan paneeli uudestaan

        });

        timer.start();

    }

    public double HourAverageTemp() {

        double dAverageTemp = TempList.stream().mapToDouble(val -> val).average().orElse(0.0); // keskiarvon lasku average() :lla 
        // System.out.println(HourAverageTemp());
        // System.out.println("Viime tunnin keskiarvo lisätty tiedostoon!"); // Viime tunnin keskiarvo
        //dAverageTemp = Double.valueOf(dAverageTemp); // 2 desimaalin tarkkuus
        //System.out.println(dAverageTemp);
        return dAverageTemp;
    }

    public double LowestTemp() {

        double dLowestTemp = TempList.stream().mapToDouble(val -> val).min().orElse(0.0); // keskiarvon lasku average() :lla 
        // dLowestTemp = Double.valueOf(df.format(dLowestTemp)); // 2 desimaalin tarkkuus
        return dLowestTemp;
    }

    public double HighestTemp() {

        double dHighestTemp = TempList.stream().mapToDouble(val -> val).max().orElse(0.0); // keskiarvon lasku average() :lla 
        // dHighestTemp = Double.valueOf(df.format(dHighestTemp)); // 2 desimaalin tarkkuus
        return dHighestTemp;
    }

    class Display extends JPanel {

        @Override
        public void paintComponent(Graphics g) {

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 1000, 90);    // tausta valkoinen
            g.setColor(color);
            g.setFont(new Font("TimesRoman", Font.PLAIN, 30));// fontin koko
            g.drawString(value, 10, 50); // teksti

        }
    }

    // tavut merkkijonoksi
    public static String BytesToString(byte[] bytes, int len) {
        String txt = "";
        for (int i = 0; i < len; i++) // muunnetaan tavut merkkijonoksi
        {
            txt += (char) bytes[i];
        }
        return txt;
    }

    // kellonajan asetuskomento Khh mm ss
    public static String MakeTimeSetCommand() {
        LocalDateTime now = LocalDateTime.now(); // the date & time now

        String h, m, s;

        // add leading zeroes to time values if under 10
        if (now.getHour() >= 10) {
            h = Integer.toString(now.getHour());
        } else {
            h = "0" + Integer.toString(now.getHour());
        }
        if (now.getMinute() >= 10) {
            m = Integer.toString(now.getMinute());
        } else {
            m = "0" + Integer.toString(now.getMinute());
        }
        if (now.getSecond() >= 10) {
            s = Integer.toString(now.getSecond());
        } else {
            s = "0" + Integer.toString(now.getSecond());
        }

        return h + " " + m + " " + s + "\n"; // command string  
    }

    public static String MakeDateSetCommand() {
        LocalDateTime now = LocalDateTime.now(); // the date & time now

        String day, month, year;

        day = Integer.toString(now.getDayOfMonth());
        month = Integer.toString(now.getMonthValue());
        year = Integer.toString(now.getYear());

        return "K" + day + " " + month + " " + year + "\n"; // command string  
    }

    public static int HourForChart() {
        String h, m, s;
        LocalDateTime now = LocalDateTime.now(); // the date & time now    

        // add leading zeroes to time values if under 10
        if (now.getHour() >= 10) {
            h = Integer.toString(now.getHour());
        } else {
            h = "0" + Integer.toString(now.getHour());
        }

        int hourInt = Integer.parseInt(h);
        return hourInt; // command string  
    }

    public static int MinuteForChart() {
        String h, m, s;
        LocalDateTime now = LocalDateTime.now(); // the date & time now    

        // add leading zeroes to time values if under 10
        if (now.getMinute() >= 10) {
            h = Integer.toString(now.getMinute());
        } else {
            h = "0" + Integer.toString(now.getMinute());
        }

        int minuteInt = Integer.parseInt(h);
        return minuteInt; // command string  
    }

    public static int SecondForChart() {
        String h, m, s;
        LocalDateTime now = LocalDateTime.now(); // the date & time now    

        // add leading zeroes to time values if under 10
        if (now.getSecond() >= 10) {
            s = Integer.toString(now.getSecond());
        } else {
            s = "0" + Integer.toString(now.getSecond());
        }

        int secondInt = Integer.parseInt(s);
        return secondInt; // command string  
    }

    public void FirstTempWhenNowPressed() {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;

        Serial.SendLineToComPort("L\n"); // pyydetään Arduinolta lämpötila
        try {
            Thread.sleep(100L); // hieman vastausaikaa eli 100ms

        } catch (InterruptedException ex) {
        }

        if ((len = Serial.GetLineFromComPort(buffer, BUFFER_SIZE)) > 0) {
            value = BytesToString(buffer, len); // näytetään, mitä Arduino vastaa
        }
        value = BytesToString(buffer, len); // näytetään, mitä Arduino vastaa

        String valueParse = value.replaceAll("[^0-9]", "");
        double valueDouble = Double.parseDouble(valueParse) / 10;
        String minutes = (MinuteForChart() < 10 ? "0" : "") + MinuteForChart();
        
        Minute currentTime = new Minute();

     
        series1.addOrUpdate(currentTime, valueDouble);

        if (HowManyCharts2 < 1) { // Vain ensimmäinen chartin avaus lisätään TempListiin jne.
            // Tunnin vaihtumisen ehto, jos minuutit yli 2, lasketaan viime tunnin lämpötilojen keskiarvo TempList:stä ja kirjoitetaan tunti ja keskiarvo tiedostoon
            if (MinuteForChart() > 2) {

                // Lisätään templistiin lämpötila
                TempList.add(valueDouble);

                // Tulostetaan kellonaika sekä ArrayList, jossa lämpötilat ovat 
                // String minutes = (MinuteForChart() < 10 ? "0" : "") + MinuteForChart();
                System.out.println("Klo " + HourForChart() + "." + minutes + "\t" + TempList);
                // System.out.println(TempList);

            } else if (MinuteForChart() <= 2) {
                try {
                    // kutsutaan funktiota joka laskee edellisen tunnin lämpötilojen keskiarvon (TempList) ja kirjoittaa tiedostoon tunnin sekä ka lämpötilan
                    MakeFile();
                    System.out.println("Viime tunnin lämpötilojen keskiarvo " + HourAverageTemp() + " lisätty tiedostoon!");
                    System.out.println("Viime tunnin korkein lämpötila oli " + HighestTemp() + " ja alhaisin lämpötila oli " + LowestTemp() + " astetta.");
                } catch (IOException ex) {
                    Logger.getLogger(Arduino.class.getName()).log(Level.SEVERE, null, ex);
                }
                // Tyhjennetään TempList, seuraavan tunnin lämpötiloja varten
                TempList.clear();
                System.out.println("TempList tyhjennetty seuraavan tunnin lämpötiloja varten.");
                TempList.add(valueDouble); // Uuteen listaan ensimmäinen arvo
                System.out.println("Klo " + HourForChart() + "." + minutes + "\t" + TempList);
            }
        }
    }

    public static void main(String[] args) {

        // onnistuuko sarjaportin käyttö?
        if (Serial.connect(PORT, SPEED)) {

            Arduino win = new Arduino(); //käyttöliittymä päälle

            //laitetaan Arduino kello aikaan
            Serial.SendLineToComPort(MakeTimeSetCommand());
            Serial.SendLineToComPort(MakeDateSetCommand());

        } else {
            value = "\n" + PORT + " reserved or does not exist!";
        }
    }
}
