package com.company;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {

    public String getHasErrorString() {
        return HasErrorString;
    }

    private static String HasErrorString;
    private static String LN=System.lineSeparator();


    public static void main(String[] args) {

        if (args.length < 2)  {
            System.out.println("Usage java OverlapDetectorReport.jar Error_file ZNI_List_File");
            return;
        }

        // Фалы со строками ошибок и строками списка ЗНИ и почтоых адресов
        List<String> ErrFileLines=LoadFile(args[0]);
        HashMap<String, List<String>> EmailList=LoadEmailList(args[1]);


        // List of
        List<String> txtMailBody=new ArrayList<String>();
        List<String> txtMailAddress=new ArrayList<String>();

        boolean flagLookNextString=false;
        String sMainZNI=new String();
        String sOverlapZNI=new String();
        String MailFile= Paths.get(Paths.get(args[0]).getParent().toString(),"ODMail.txt").toString();
        String AddressFile = Paths.get(Paths.get(args[0]).getParent().toString(),"ODAdress.txt").toString();

        // Сгенерируем отчет по файлу ошибок
        for (String line : ErrFileLines)
        {
            String[] items = line.split(",");
            String sReportString=new String();

            if ((items.length<2) & !flagLookNextString)
                    continue;

            if (!flagLookNextString) {
                if (!items[1].equals(sMainZNI) & (items[0].equals("1") | items[0].equals("2") | items[0].equals("4"))) {
                    sReportString = LN+"ЗНИ " + items[1] + " разработчик " + items[2];
                    sMainZNI = items[1];
                    sOverlapZNI="";
                    txtMailBody.add(sReportString);
                    if (EmailList.containsKey(sMainZNI))
                    {
                        List<String> lstEmails=EmailList.get(sMainZNI);
                        lstEmails.forEach(s->txtMailAddress.add(s+";"));
                    }
                }
            }

            switch (items[0]) {
                case "1":
                    sReportString="   "+items[3]+" не вошла в сборку , но указана зависимость";
                    break;
                case "2":
                    if (!items[3].equals(sOverlapZNI)) {
                        sReportString =  "  Имеет пересечения не отмеченные в порядке наката с " + items[3] + " " + items[4]+LN;
                        sOverlapZNI=items[3];
                    };
                    sReportString=sReportString+"      "+items[5];
                    break;
                case "3":
                    sReportString=LN+LN+" Ошибка при разборе Install.txt";
                    if (items[1].length()>0)
                         sReportString=sReportString+" по ЗНИ "+items[1];
                    else
                        sReportString=sReportString+" по файлу "+items[2];
                    flagLookNextString=true;
                    sMainZNI="";
                    sOverlapZNI="";
                    break;
                case "4":
                    sReportString=sReportString+"    Сообщите о изменении кода разработчкам ЗНИ "+items[3];
                    if (EmailList.containsKey(items[3]))
                    {
                        List<String> lstEmails=EmailList.get(items[3]);
                        sReportString=sReportString+" "+lstEmails.get(0);
                    };
                    break;
            }
            if (flagLookNextString & items.length==1)
            {
                sReportString="   "+items[0]+LN;
                flagLookNextString=false;

            }
            txtMailBody.add(sReportString);

        }


        txtMailBody.forEach(System.out::println);

        SaveFile(MailFile, txtMailBody);
        SaveFile(AddressFile, txtMailAddress);

    }

    // Загрузить файл с диска с обработкой ошибок
    private static List<String> LoadFile(String FileName)
    {
        List<String> lines = new ArrayList<>();

        if (!Files.exists(Paths.get(FileName)))
        {
            System.out.println("File not found " + FileName);
            HasErrorString = HasErrorString + "File not found " + FileName + System.lineSeparator();
        }
        else {
            try {
                lines = Files.readAllLines(Paths.get(FileName), Charset.forName("windows-1251"));
            } catch (IOException e) {
                System.out.println("IO Error reading file " + FileName);
                System.out.println(e.getMessage());
                HasErrorString = HasErrorString + "IO error reading File " + FileName + System.lineSeparator();
            }
        }
        return lines;
    }

    // Сохранить список в файл
    private static void SaveFile(String fileName, List<String> ReportText)
    {

        try
        {
            Files.write(Paths.get(fileName), ReportText, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
        catch (IOException e)
        {
            System.out.println("IO Error writing Objects File "+fileName);
            System.out.println(e.getLocalizedMessage());
            System.out.println(e.getMessage());
            System.out.println(e.fillInStackTrace());
        }
    }

    // Загрузить список ЗНИ списком
    private static HashMap<String, List<String>> LoadEmailList(String fromFile)
    {
        List<String> ZNIListLines=LoadFile(fromFile);
        HashMap<String, List<String>> EmailList=new HashMap<String, List<String>>();

        for (String ZNIItem : ZNIListLines)
        {
            List<String> stringMails=new ArrayList<String>();
            String keyZNI=new String();
            String[] items=ZNIItem.split(",");
            int idx=0;

            while (idx<items.length & !items[idx].equals("#"))
            {
                if (idx==0)
                    keyZNI=items[idx];
                if (idx>2)
                    stringMails.add(items[idx]);
                idx++;
            };
            EmailList.put(keyZNI,stringMails);
        }
        return EmailList;
    }
}
