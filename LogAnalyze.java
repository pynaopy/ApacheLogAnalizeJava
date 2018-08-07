import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * access.log解析
 * @author (RKKCS)N.Nakano
 *
 */
public class LogAnalyze {

    /* 定数群 */
    // 時刻文字列の番号
    static final int TIME_INDEX = 4;
    // 省略対象正規表現
    static String P_STR = "HealthCheck|ServiceDiagTool";
    static String P_SYM = "[\\[\\]\\\"]";
    // 読み込みファイル取得
    static SimpleDateFormat DATA_SDF = new SimpleDateFormat("yyyyMMdd"); // 日付フォーマット
    static SimpleDateFormat TIME_SDF = new SimpleDateFormat("HHmmss");   // 時刻フォーマット
    static SimpleDateFormat US_SDF = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss", Locale.ENGLISH); // USフォーマット

    /**
     * メインクラス
     * @param args
     */
    public static void main(String[] args){

        long l = System.currentTimeMillis();
        // 引数Check
        if (args.length != 3){
            System.out.println("! ERROR! This program needs [ThisFile] [LogFile] [InfoFile] [outputDir].");
            System.exit(1);
        }else{
            BufferedReader iInfBr = null;
            BufferedReader iLogBr = null;
            BufferedWriter oBw = null;
            try{

                // ファイル取得
                File iLogFile = new File(args[0]); // 変換元ファイル
                File iInfFile = new File(args[1]); // ホスト情報ファイル
                File oCsvDir = new File(args[2]); // ホスト情報ファイル
                iLogBr = new BufferedReader(new FileReader(iLogFile));
                iInfBr = new BufferedReader(new FileReader(iInfFile));

                System.out.println("@ FILE OPENED!");

                String line = "";
                String infLine = "";
                List<String> infTokens = new ArrayList<String>();
                StringBuffer result = new StringBuffer();
                List<Integer> validTokens = new ArrayList<Integer>();
                long lineCnt = 0;
                validTokens.addAll(Arrays.asList( Integer.valueOf(1),
                                                  Integer.valueOf(4),
                                                  Integer.valueOf(6),
                                                  Integer.valueOf(7),
                                                  Integer.valueOf(9),
                                                  Integer.valueOf(10),
                                                  Integer.valueOf(11) ));
                // ホスト情報読み込み
                while( infLine != null ){
                    infLine = iInfBr.readLine();
                    if (infLine == null) break;
                    infTokens.add(infLine);
                }
                if (infTokens.size() != 2){
                    System.out.println("! ERROR! APInfo IS WRONG.");
                    System.exit(1);
                }

                Matcher mDate = Pattern.compile("\\d{8}").matcher(args[0]);
                String logDate;
                if (mDate.find()){
                    logDate = "_" + mDate.group();
                }else{
                    logDate = "";
                }
                //出力先設定
                String csvFilePath = "";
                if (!oCsvDir.exists()){
                    oCsvDir.mkdirs();
                }
                csvFilePath = oCsvDir.getAbsolutePath()+ File.separator + infTokens.get(0) + logDate + ".csv";
                File oFile = new File( csvFilePath );
                oBw = new BufferedWriter(new FileWriter(oFile));
                System.out.println("@ APInfo LOAD COMPLETE");
                // 解析対象ファイル情報出力
                System.out.println("@ [ TARGET : "+ csvFilePath + " ]");

                // 解析開始
                System.out.println("@ START LOG ANALYZE");
                while(line != null){
                    line = iLogBr.readLine();
                    if (line == null) break;
                    Matcher m = Pattern.compile(P_STR).matcher(line);
                    // 不要な文字列を含む行以外を編集
                    if (!m.find()) {
                        result = editLine(line, infTokens, P_SYM, validTokens);
                        oBw.write(result.toString());
                        oBw.newLine();
                    }

                    // 進捗バーの表示
                    lineCnt++;
                    if (lineCnt % 10000 == 0){
                        System.out.print("#");
                    }
                }
                System.out.println("\n");
                oBw.close();
            }catch(Exception e){
                System.out.println("! ERROR! FAILED LOG ANALYZE.");
                System.out.println("!        SEE THE STACK TRACE.");
                e.printStackTrace();
                System.exit(1);
            }
        }
        System.out.println("@ LOG ANALYZE COMPLETE !");
        System.out.println("@ PROCCESS TIME："+ (System.currentTimeMillis() - l) +" ms");
        System.out.println("\n-----------------------------------");
    }

    /**
     * 行編集
     * @param 元行
     * @param 削除記号正規表現
     * @param 切り分け後トークン配列
     * @param 有効トークンindexリスト
     * @param 日付フォーマット
     * @param 時刻フォーマット
     * @param USフォーマット
     */
    private static StringBuffer editLine (String line,
                                          List<String> infTokens,
                                          String pSymbol, 
                                          List<Integer> validTokens) {
        String[] tokens = {};
        String dateStr;
        String timeStr;
        Date UsDate;
        StringBuffer result = new StringBuffer();
        StringBuffer sbSpace = new StringBuffer();
        // 不要な記号を排除
        line = line.replaceAll(pSymbol, "");
        if (line.length() > 0){
            tokens = line.split(" ");
            // 不要なトークンを排除
            for (int i=0;i<tokens.length;i++){
                sbSpace.setLength(0);
                if(validTokens.contains(Integer.valueOf(i+1))){
                    // 有効トークンリストのindexの文字列のみ取得
                    if(tokens[i].equals("-")){
                        tokens[i] = "";
                    }
                    if((i+1) == TIME_INDEX){
                        try{
                            // 時刻をデータ用のフォーマットに編集
                            UsDate = US_SDF.parse(tokens[i]);
                            dateStr = DATA_SDF.format(UsDate);
                            timeStr = TIME_SDF.format(UsDate);
                            tokens[i] = sbSpace.append(dateStr)
                                               .append("\",\"")
                                               .append(timeStr).toString();
                        }catch(ParseException pe){
                            pe.printStackTrace();
                            System.out.println("! ERROR! DATA EDIT FAILED.");
                            System.exit(1);
                        }
                    }
                    if(Integer.valueOf(i+1).equals(validTokens.get(0))){
                        // 最初のトークンの前にサーバー情報を連結
                        result.append("\"").append(infTokens.get(0)).append("\"")
                              .append(",").append("\"").append(infTokens.get(1)).append("\"");
                    }
                    // ダブルクォートで囲んで連結
                    result.append(",");
                    result.append("\"");
                    result.append(tokens[i]);
                    result.append("\"");
                }
            }
        }
        return result;
    }
}
