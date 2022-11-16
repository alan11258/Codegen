package codegen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 *	作 業 代 碼 ：CustomCodegen<br>
 *	作 業 名 稱 ：Custom Codegen for Special Framework<br>
 *	程 式 代 號 ：CustomCodegen.java<br>
 *	描 述 ：Custom Codegen Generator, 使用前須設定載入ojdbc.jar<br>
 *	公 司 ： Tenpastten Studio<br>
 *	【 資 料 來 源】 ：<br>
 *	【 異 動 紀 錄】 ：[2022-07-28] runCodegen 方法使用 Java 1.8 重構，原runCodegen 更名為 sample_three By Alan Hsu<br>
 *	@author : Alan Hsu<br>
 *	@version : 1.0.0 2022-05-10<br>
 */
public class CustomCodegen {
	
	private final static String CODEGEN_VERSION = "1.1.16";
	
	private final static String Oracle_SERVER_URL = "jdbc:oracle:thin:@10.XXX.XXX.XX:15XX:XXXXDB";
	
	private final static String MySQL_SERVER_URL = "jdbc:mysql://localhost:3306/XXXX?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
	
	private final static String Oracle_ACCOUNT = "oracle_mgr";
	private final static String Oracle_PWD = "oracle_mgr";
	private final static String MySQL_ACCOUNT = "mysql_mgr";
	private final static String MySQL_PWD = "mysql_mgr";
	
	private final static int MAX_RETRY_TIMES = 5;

	/** 定義表格內欄位資訊 */
	class ColumnInfo {
		
		private String columnName;
		
		private String selfColumnName;
		
		private String remarks;
		
		private String columnType;
		
		private String javaType;
		
		private boolean isPrimaryKey;

		public String getColumnName() {
			return columnName;
		}

		public void setColumnName(String columnName) {
			this.columnName = columnName;
		}

		public String getSelfColumnName() {
			return selfColumnName;
		}

		public void setSelfColumnName(String selfColumnName) {
			this.selfColumnName = selfColumnName;
		}

		public String getRemarks() {
			return remarks;
		}

		public void setRemarks(String remarks) {
			this.remarks = remarks;
		}

		public String getColumnType() {
			return columnType;
		}

		public void setColumnType(String columnType) {
			this.columnType = columnType;
		}

		public String getJavaType() {
			return javaType;
		}

		public void setJavaType(String javaType) {
			this.javaType = javaType;
		}

		public boolean isPrimaryKey() {
			return isPrimaryKey;
		}

		public void setPrimaryKey(boolean isPrimaryKey) {
			this.isPrimaryKey = isPrimaryKey;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("");
			sb.append( "Field:" + ( selfColumnName != null ? selfColumnName : columnName )  );
			sb.append( " - " + columnType + " - " );
			sb.append( javaType + " - Remarks:" + remarks );
			sb.append( ( isPrimaryKey ? " - Is Primary Key" : "" ) );
			return sb.toString();
		}
		
	}

	/** 定義表格資訊 */
	class TableInfo {
		
		private String tableName;
		
		private boolean hasPrimaryKey;
		
		private int columnCount;
		
		private Map<String,ColumnInfo> columnMap;
		
		TableInfo(){}
		
		TableInfo( String tableName ) {
			this.tableName = tableName;
		}
		
		public Map<String,ColumnInfo> getColumns() {
			return columnMap;
		}

		public void setColumns(Map<String,ColumnInfo> columnMap) {
			this.columnMap = columnMap;
		}

		public Boolean getHasPrimaryKey() {
			return hasPrimaryKey;
		}

		public void setHasPrimaryKey(Boolean hasPrimaryKey) {
			this.hasPrimaryKey = hasPrimaryKey;
		}
		
		public int getColumnCount() {
			return columnCount;
		}

		public void setColumnCount(int columnCount) {
			this.columnCount = columnCount;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		public String getTableName() {
			return tableName;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Table:" + tableName );
			sb.append( " - Select Column Count:" + columnCount );
			sb.append( ( hasPrimaryKey ? " - Has Primary Key" : "" ) + System.lineSeparator() );
			sb.append( System.lineSeparator() );
			/**
			for( Map.Entry<String, ColumnInfo> entry : columnMap.entrySet() ) {
				sb.append( entry.getKey() + " > " );
				sb.append( entry.getValue() + System.lineSeparator() );
			}
			*/
			columnMap.entrySet().stream().forEach( c -> {
				sb.append( c.getKey() + " > " );
				sb.append( c.getValue() );
				sb.append( System.lineSeparator() );
			});
			return sb.toString();
		}
		
	}
	
	/** 目前套件路徑  */
	/** private String packagePath; */
	
	/** 作業代碼  */
	private String taskId;
	
	/** 作業名稱  */
	private String taskName;
	
	/** 程式說明區塊-描述  */
	private String taskDescription;
	
	/** 程式說明區塊-資料來源  */
	private String sourceDescription;
	
	/** 表格名稱 */
	private String tableName;
	
	/** 表格Schema */
	private String tableSchema;
	
	/** 查詢欄位 */
	private String columns;
	
	/** 作者 */
	private String author;
	
	/** 連線資料庫 */
	private String dbName;
	
	/** 產製檔案名稱 */
	private String domainObjectName;
	
	/** 產製檔案輸出位置 */
	private String newFileTargetFolder;
	
	/** 產製Bean名稱 */
	private String domainBeanName;
	
	/** 產製Dao名稱 */
	private String domainDaoName;
	
	/** 產製IDao名稱 */
	private String domainIDaoName;
	
	/** 目標Bean套件路徑 */
	private String targetBeanPackage;
	
	/** 目標Dao套件路徑 */
	private String targetDaoPackage;
	
	/** 目標IDao套件路徑 */
	private String targetIDaoPackage;
	
	/** 目標Bean產檔路徑 */
	private String targetBeanPath;
	
	/** 目標Dao產檔路徑 */
	private String targetDaoPath;
	
	/** 目標IDao產檔路徑 */
	private String targetIDaoPath;
	
	/** 是否有BigDecimal型態 */
	private boolean hasBigDecimal;
	
	/** 是否有TIMESTAMP型態 */
	private boolean hasTimeStamp;
	
	/** 是否有日期型態 */
	private boolean hasDate;
	
	/** 命名原則 */
	private String namingConventions;
	
	/** 映射註解  Annotation \@Basic or \@Column */
	private String mappedType;
	
	/** 是否產檔 Dao & IDao */
	private boolean needDao;
	
	/** Entity 是否產生override toString 的方法 */
	private boolean needToString;
	
	/** 現在時間 */
	private String currentTime;
	
	/** 表格欄位資訊 */
	private TableInfo tableInfo;

	/**
	 *	sample_one 說明：範例一, 示範產生Entity, Dao, Interface<br>
	 *	@author Alan Hsu
	 */
	public void sample_one(CustomCodegen gen) throws Exception {
		/** 作者 */
		gen.setAuthor("Alan");
		/** 產出檔案名稱 */
		gen.setDomainObjectName("SCTest");
		/** 產製檔案輸出位置 */
		gen.setNewFileTargetFolder("D:\\Codegen\\output");
		/** DB連線設定,預設在MySQL */
		gen.setDbName("MySQL");
		/** 來源DB表格 */
		gen.setTableName("sctype");
		/** 作業(選單)代號 */
		gen.setTaskId("TEST001");
		/** 作業(選單)名稱 */
		gen.setTaskName("測試查詢");
		/** 程式說明區塊-描述 */
		gen.setTaskDescription("測試查詢功能");
		/** 程式說明區塊-資料來源 */
		gen.setSourceDescription("測試查詢(TestQuery)");
		/** 產出Entity目標路徑 */
		gen.setTargetBeanPath(
				"D:\\workspace\\bean");
		/** 產出Entity Annotation 類型：  1 > \@Column or 2 > \@Basic*/
		gen.setMappedTypeByCode(1);
		/** Entity 是否產生override toString 的方法 */
		gen.setNeedToString( true );
		/** 是否產出Dao, true：產出Entity & Dao & IDao, false：僅產出Entity */
		gen.setNeedDao( true );
		/** 產出Dao目標路徑 */
		gen.setTargetDaoPath(
				"D:\\workspace\\impl");
		/** 產出IDao目標路徑 */
		gen.setTargetIDaoPath("D:\\workspace\\dao");
		/** 必要設定檢核 */
		if( gen.checkRequiredSetting() ) {
			/** 開始Codegen */
			gen.excute();
		}
	}

	/**
	 *	sample_two 說明：範例二, 只產Entity並示範使用自行設定的欄位數量<br>
	 *	@author Alan Hsu
	 *	@throws Exception
	 */
	public void sample_two( CustomCodegen gen ) throws Exception {
		/** 作者 */
		gen.setAuthor("Alan");
		/** 產出檔案名稱 */
		gen.setDomainObjectName("SCTest");
		/** 產製檔案輸出位置 */
		gen.setNewFileTargetFolder("D:\\Codegen\\output");
		/** DB連線設定,預設在MySQL */
		gen.setDbName("MySQL");
		/** 來源DB表格 */
		gen.setTableName("sctype");
		/** Menu 欄位有 category, menuId, parentMenuId, taskId, orderNo */
		String columns = "category, menuId, parentMenuId, taskId";
		/** 來源表格欄位 - 用來客製化 1.產生的屬性大小寫命名 2.產生的屬性數量(非產生全部Table的欄位) */
		gen.setColumns(columns);
		/** 作業(選單)代號 */
		gen.setTaskId("TEST002");
		/** 作業(選單)名稱 */
		gen.setTaskName("選單查詢測試");
		/** 程式說明區塊-描述 */
		gen.setTaskDescription("測試查詢選單功能");
		/** 程式說明區塊-資料來源 */
		gen.setSourceDescription("選單(Menu)");
		/** 產出Entity目標路徑 */
		gen.setTargetBeanPath(
				"D:\\workspace\\bean");
		/** 產出Entity Annotation 類型：  1 > \@Column or 2 > \@Basic*/
		gen.setMappedTypeByCode(2);
		/** Entity 是否產生override toString 的方法 */
		gen.setNeedToString( true );
		/** 是否產出Dao, true：產出Entity & Dao & IDao, false：僅產出Entity */
		gen.setNeedDao( false );
		/** 產出Dao目標路徑 */
		/**
		gen.setTargetDaoPath(
				"D:\\workspace\\impl");
		*/
		/** 產出Interface目標路徑 */
		/**
		 *	gen.setTargetInterfacePath("D:\\workspace\\dao");
		 */
		
		/** 必要設定檢核 */
		if( gen.checkRequiredSetting() ) {
			/** 開始Codegen */
			gen.excute();
		}
	}
	
	/**
	 * sample_three 說明：範例三, 使用自行設定的欄位數量來產生Entity, Dao, IDao<br>
	 * 					以及自訂檔案輸出位置, 使用IDE手動在main方法執行<br>
	 * 
	 * @author Alan Hsu
	 * @throws Exception 
	 */
	public void sample_three( CustomCodegen gen ) throws Exception {
		/** 作者 */
		gen.setAuthor("Alan");
		/** 產出檔案名稱 */
		gen.setDomainObjectName("SCTest");
		/** 產製檔案輸出位置 */
		gen.setNewFileTargetFolder("D:\\Codegen\\output");
		/** DB連線設定,預設在MySQL */
		gen.setDbName("MySQL");
		/** 來源DB表格 */
		gen.setTableName("Menu");
		/** Menu 欄位有 category, menuId, parentMenuId, taskId, orderNo */
		String columns = "category, menuId, parentMenuId, taskId";
		
		/** 來源表格欄位 - 若不為NULL即開啟客製化 1.產生的屬性大小寫命名 2.產生的屬性數量(非產生全部Table的欄位) */
		gen.setColumns( columns );
		
		/** 作業(選單)代號 */
		gen.setTaskId("TEST003");
		/** 作業(選單)名稱 */
		gen.setTaskName("選單查詢測試");
		/** 程式說明區塊-描述 */
		gen.setTaskDescription("選單查詢測試功能");
		/** 程式說明區塊-資料來源 */
		gen.setSourceDescription("選單查詢測試(Menu)");
		/** 產出Entity目標路徑 */
		gen.setTargetBeanPath("D:\\workspace\\bean");
		/** 產出Entity Annotation 類型：  1 > \@Column or 2 > \@Basic*/
		gen.setMappedTypeByCode(2);
		/** Entity 是否產生override toString 的方法 */
		gen.setNeedToString( true );
		/** 是否產出Dao, true：產出Entity & Dao & IDao, false：僅產出Entity */
		gen.setNeedDao( false );
		/** 產出Dao目標路徑 */
		/**
		gen.setTargetDaoPath(
				"D:\\workspace\\impl");
		*/
		/** 產出IDao目標路徑 */
		/**
		 *	gen.setTargetInterfacePath("D:\\workspace\\dao");
		 */
		
		/** 必要設定檢核 */
		if( gen.checkRequiredSetting() ) {
			/** 開始Codegen */
			gen.excute();
		}
	}
	
	/**
	 * runCodegen 說明：Jar執行Java 1.8 Vesrsion Codegen<br>
	 * 
	 * @author Alan Hsu
	 * @throws Exception 
	 */
	public static void runCodegen() throws Exception {
		
		@SuppressWarnings("unchecked")
		ThreeParameterPredicate<Scanner, Map<String,Object>, CustomCodegen, Boolean> predicateThree 
			= ( sc, topic, custom ) -> {
				
				/** The Function here is designed to check optional settings */
				Function<CustomCodegen,Boolean> function = 
						(Function<CustomCodegen, Boolean>) topic.get("function");
				
				/** If optional settings is false then return true to by pass below question */
				if( function != null && !function.apply( custom ) )return true;
				
				Predicate<String> predicate = 
						(Predicate<String>) topic.get("predicate");
				BiConsumer<CustomCodegen,String> biconsumer = 
						(BiConsumer<CustomCodegen, String>) topic.get("biconsumer");
				
				String name = (String) topic.get("name");
				String eg = (String) topic.get("eg");
				String hint = (String) topic.get("hint");
				
				String example = "";
				if( eg != null ) {
					if( eg.length() > 20 ) {
						example = System.lineSeparator() + "(E.g. " + eg + ')';
					} else {
						example = "(E.g. " + eg + ')';
					}
				}
				
				System.out.print( System.lineSeparator() );
				
				String instruct = "請輸入【" + name + '】' + example + '：';
				
				System.out.print( instruct );
				String nextLine = sc.nextLine();
				
				if( nextLine == null ) {
					System.out.print( "未輸入任何文字，" + instruct );
				}
				
				String userInput = nextLine.trim();
				
				int maxRetry = MAX_RETRY_TIMES, retryCount = 0;
				
				while( userInput != null ) {
					
					if( checkEsc( userInput ) ) {
						return false;
					}
					
					if( predicate.test( userInput ) ) {
						biconsumer.accept( custom, userInput );
						return true;
					} else {
						
						retryCount++;
						
						if( "".equals( userInput ) ) {
							System.out.print( System.lineSeparator() );
							System.out.println("您輸入的為空值");
						} else {
							String incorrect = "您輸入的【" + name + "】不正確";
							String errorMsg = retryCount == maxRetry ? incorrect : incorrect + '，' + hint + '！';
							System.out.print( System.lineSeparator() );
							System.out.println( errorMsg );
						}
						
						if( retryCount == maxRetry ) {
							System.err.print( System.lineSeparator() );
							System.err.println( "### The number of retry times is " + retryCount + " ###");
							System.err.println( "### Codegen process terminated due to reached max retry limit ###");
							return false;
						}
						
						System.out.print("請重新輸入【" + name + "】" + example + "：");
						userInput = sc.nextLine();
					}
					
				}
				return false;
		};
		
		CustomCodegen cusg = new CustomCodegen();
		List<Map<String,Object>> topics = getTopic();
		
		try( Scanner sc = new Scanner( System.in ) ) {
			
			System.out.print( System.lineSeparator() );
			System.out.println( "=================================================================" );
			System.out.println( "If you want to end the process, please enter \"esc\" or \"quit\"." );
			System.out.println( "=================================================================" );
			
			boolean result = topics.stream().allMatch( m -> predicateThree.test( sc, m, cusg ) );
			
			System.out.print( System.lineSeparator() );
			
			System.out.println("Final result is " + ( result ? "Success" : "Failed" ) );
			
			System.out.println( System.lineSeparator() );
			
			/** 必要設定檢核 */
			if( result && cusg.checkRequiredSetting() ) {
				/** 開始Codegen */
				cusg.excute();
			}
			
		} catch( Exception e ) {
			System.err.println( "### Scanner occur exception ###");
			e.printStackTrace();
		}
	}
	
	/**
	 * getCurrentTimeStr 說明：取得台北時區現在的年月日時分秒字串<br>
	 * 
	 * @return String nowStr
	 * @author Alan Hsu
	 */
	public static String getCurrentTimeStr() {
		
		/**
		Date now = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		*/
		
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		ZonedDateTime now = ZonedDateTime.now( ZoneId.of("Asia/Taipei") );
		
		String nowStr = now.format( dtf );
		
		return nowStr;
	}
	
	/**
	 * checkEsc 說明：檢查是否輸入結束程序之指令<br>
	 * 
	 * @return boolean hasEsc
	 * @author Alan Hsu
	 */
	public static boolean checkEsc( String userInput ) {
		if( "quit".equals( userInput.toLowerCase() ) || "esc".equals( userInput.toLowerCase() ) ) {
			System.err.println( "### Codegen process terminated Successful ###");
			return true;
		}
		return false;
	}
	
	/**
	 * getTopic 說明：取得輸入題目和檢核條件，以及要設值的bean方法<br>
	 * 
	 * @return List<Map<String,Object>> topicList
	 * @author Alan Hsu
	 */
	@SuppressWarnings({ "unused", "serial" })
	public static List<Map<String,Object>> getTopic() {
		List<Map<String,Object>> topicList = new LinkedList<>();
		
		Predicate<String> isFolderPath = 
				pathStr -> pathStr.matches("^[a-zA-Z]{1}:(\\\\|\\/\\/)[\\w\\/\\\\_-]*$");
		Predicate<String> isFolderPathIncludeChinese = 
				pathStr -> pathStr.matches("^[a-zA-Z]{1}:(\\\\|\\/\\/)[\\w\\/\\\\_\\-\\u4e00-\\u9fa5]*$");
		
		Predicate<String> isPackagePath = pathStr -> pathStr.matches("^[a-zA-Z\\d]+[\\da-zA-Z_\\.\\\\]*$");
		
		Predicate<String> isWordNum = str -> str.matches("^[\\w]+$");
		Predicate<String> isWordNumSpace = str -> str.matches("^[\\w\\s]+$");
		Predicate<String> isWordNumCommaSpace = str -> str.matches("^[\\w\\,\\s]+$");
		
		Predicate<String> isCapitalWord = str -> str.matches("^[A-Z]+$");
		Predicate<String> isCapitalWordAndNum = str -> str.matches("^[A-Z\\d]+$");
		Predicate<String> isNum = numStr -> numStr.matches("^[\\d]+$");
		Predicate<String> isOneOrTwo = str -> str.matches("(1|2)");
		Predicate<String> isYesOrNo = str -> str.matches("(Y|N|y|n)");
		Predicate<String> mysqlOrOracle = str -> str.matches("(MYSQL|ORACLE|MySQL|Oracle|mysql|oracle)");
		
		Predicate<String> isChinese = str -> str.matches("^[\\u4e00-\\u9fa5]+$");
		Predicate<String> isChinese2 = str -> str.matches("^[\\u4E00-\\u9FFF]+$");
		Predicate<String> isFullwidthNum = str -> str.matches("^[\\uFF10-\\uFF19]+$");
		Predicate<String> isSmallCapitalEng = str -> str.matches("^[\\uFF41-\\uFF5A]+$");
		Predicate<String> isCapitalEng = str -> str.matches("^[\\uFF21-\\uFF3A]+$");
		
		Predicate<String> isWordNumAndChinese = str -> str.matches("^[\\w\\u4e00-\\u9fa5]+$");
		Predicate<String> isWordNumChineseAndMark = 
				str -> str.matches("^[\\w\\u4e00-\\u9fa5()\\\\\\/_]+$");
		Predicate<String> isWordNumZhMarkCommaEtSpace = 
				str -> str.matches("^[\\w\\u4e00-\\u9fa5()\\\\\\/_\\-\\.\\,\\&\\s]+$");
		
		Predicate<String> isEmptyString = str -> str.matches("^$");
		Predicate<String> lessThenFive = strLen -> strLen.length() < 5;
		
		
		String fieldsSpace = "\t";
		
		BiConsumer<CustomCodegen,String> setAuthor = CustomCodegen::setAuthor;
		Map<String,Object> paramMap = new HashMap<String,Object>() {{
			put("name", "作者(非必填，可按Enter跳過)");
			put("eg", "Alan");
			put("predicate", isWordNumZhMarkCommaEtSpace.or( isEmptyString ) );
			put("hint", "請輸入數字或文字，可包含[ , & 空白] (非必填，可按Enter跳過)" );
			put("biconsumer", setAuthor );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setDomainObjectName = CustomCodegen::setDomainObjectName;
		paramMap = new HashMap<String,Object>() {{
			put("name", "產出檔案名稱(Entity,Dao,IDao前綴共同部分)");
			put("eg", "SalaryInventory");
			put("predicate", isWordNum );
			put("hint", "請輸入數字或文字" );
			put("biconsumer", setDomainObjectName );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setNewFileTargetFolder = CustomCodegen::setNewFileTargetFolder;
		paramMap = new HashMap<String,Object>() {{
			put("name", "產製檔案輸出位置");
			put("eg", "D:\\temp\\codegen");
			put("predicate", isFolderPathIncludeChinese );
			put("hint", "請輸入檔案要輸出的資料夾位置" );
			put("biconsumer", setNewFileTargetFolder );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setDbName = CustomCodegen::setDbName;
		paramMap = new HashMap<String,Object>() {{
			put("name", "DB連線設定");
			put("eg", "MySQL or Oracle");
			put("predicate", mysqlOrOracle.or( isEmptyString ) );
			put("hint", "請輸入大寫的MySQL或Oracle" );
			put("biconsumer", setDbName );
		}};
		topicList.add( paramMap );

		BiConsumer<CustomCodegen,String> setTableName = CustomCodegen::setTableName;
		paramMap = new HashMap<String,Object>() {{
			put("name", "來源DB表格");
			put("eg", "MENU or STUDENT");
			put("predicate", isWordNum );
			put("hint", "請輸入JDBC資料來源的DB表格" );
			put("biconsumer", setTableName );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setColumns = CustomCodegen::setColumns;
		paramMap = new HashMap<String,Object>() {{
			put("name", "SELECT的表格欄位(非必填，可按Enter跳過)");
			StringBuilder egMsg = new StringBuilder("Category, Key, Locale ");
			egMsg.append( System.lineSeparator() ).append( fieldsSpace ).append("若有輸入內容即開啟客製化");
			egMsg.append( System.lineSeparator() ).append( fieldsSpace ).append("1.產生的屬性大小寫命名 2.產生的屬性數量(非產生全部Table的欄位)");
			egMsg.append( System.lineSeparator() ).append( fieldsSpace ).append("採用預設，請按「Enter」");
			egMsg.append( System.lineSeparator() );
			put("eg", egMsg.toString() );
			put("predicate", isWordNumCommaSpace.or( isEmptyString ) );
			put("hint", "請輸入要SELECT的表格欄位(非必填，可按Enter跳過)" );
			put("biconsumer", setColumns );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setTaskId = CustomCodegen::setTaskId;
		paramMap = new HashMap<String,Object>() {{
			put("name", "作業選單代號(非必填，可按Enter跳過)");
			put("eg", "BMSPA014");
			put("predicate", isWordNumZhMarkCommaEtSpace.or( isEmptyString ) );
			put("hint", "請輸入有效的作業選單代號，可包含[ , & 空白] (非必填，可按Enter跳過)" );
			put("biconsumer", setTaskId );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setTaskName = CustomCodegen::setTaskName;
		paramMap = new HashMap<String,Object>() {{
			put("name", "作業選單名稱(非必填，可按Enter跳過)");
			put("eg", "薪轉員工下載檔維護");
			put("predicate", isWordNumZhMarkCommaEtSpace.or( isEmptyString ) );
			put("hint", "請輸入有效的作業選單名稱，可包含[ , & 空白] (非必填，可按Enter跳過)" );
			put("biconsumer", setTaskName );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setTaskDescription = CustomCodegen::setTaskDescription;
		paramMap = new HashMap<String,Object>() {{
			put("name", "程式說明區塊-描述(非必填，可按Enter跳過)");
			put("eg", "薪轉員工下載檔維護功能");
			put("predicate", isWordNumZhMarkCommaEtSpace.or( isEmptyString ) );
			put("hint", "請輸入有效的描述，可包含[ , & 空白] (非必填，可按Enter跳過)" );
			put("biconsumer", setTaskDescription );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setSourceDescription = CustomCodegen::setSourceDescription;
		paramMap = new HashMap<String,Object>() {{
			put("name", "程式說明區塊-資料來源(非必填，可按Enter跳過)");
			put("eg", "薪轉員工設定檔(SalaryDownload)");
			put("predicate", isWordNumZhMarkCommaEtSpace.or( isEmptyString ) );
			put("hint", "請輸入有效的資料來源，可包含[ , & 空白] (非必填，可按Enter跳過)" );
			put("biconsumer", setSourceDescription );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setTargetBeanPath = CustomCodegen::setTargetBeanPath;
		paramMap = new HashMap<String,Object>() {{
			put("name", "產出Entity的package對應資料夾路徑");
			put("eg", "D:\\workspace\\entity or workspace.entity");
			put("predicate", isPackagePath.or( isFolderPath ) );
			put("hint", "請輸入產出Entity的package資料夾路徑" );
			put("biconsumer", setTargetBeanPath );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setMappedTypeByCode = CustomCodegen::setMappedTypeByCode;
		paramMap = new HashMap<String,Object>() {{
			put("name", "產出Entity Annotation 類型");
			put("eg", "1:@Column, 2:@Basic");
			put("predicate", isOneOrTwo );
			put("hint", "請輸入 1 or 2" );
			put("biconsumer", setMappedTypeByCode );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setNeedToString = CustomCodegen::setNeedToString;
		paramMap = new HashMap<String,Object>() {{
			put("name", "Entity 是否產生override toString 的方法");
			put("eg", "Y:是, N:否");
			put("predicate", isYesOrNo );
			put("hint", "請輸入 Y or N" );
			put("biconsumer", setNeedToString );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setNeedDao = CustomCodegen::setNeedDao;
		paramMap = new HashMap<String,Object>() {{
			put("name", "是否產出Dao");
			put("eg", "Y:是, N:否");
			put("predicate", isYesOrNo );
			put("hint", "請輸入 Y or N" );
			put("biconsumer", setNeedDao );
		}};
		topicList.add( paramMap );
		
		Function<CustomCodegen,Boolean> isNeedDao = CustomCodegen::getNeedDao;
		
		BiConsumer<CustomCodegen,String> setTargetDaoPath = CustomCodegen::setTargetDaoPath;
		paramMap = new HashMap<String,Object>() {{
			put("name", "產出Dao的package對應資料夾路徑");
			put("eg", "D:\\workspace\\dao or workspace.dao");
			put("predicate", isPackagePath.or( isFolderPath ) );
			put("hint", "請輸入產出Dao的package資料夾路徑" );
			put("biconsumer", setTargetDaoPath );
			put("function", isNeedDao );
		}};
		topicList.add( paramMap );
		
		BiConsumer<CustomCodegen,String> setTargetIDaoPath = CustomCodegen::setTargetIDaoPath;
		paramMap = new HashMap<String,Object>() {{
			put("name", "產出IDao的package對應資料夾路徑");
			put("eg", "D:\\workspace\\idao or workspace.idao");
			put("predicate", isPackagePath.or( isFolderPath ) );
			put("hint", "請輸入產出IDao的package資料夾路徑" );
			put("biconsumer", setTargetIDaoPath );
			put("function", isNeedDao );
		}};
		topicList.add( paramMap );
		
		return topicList;
	}
	
	/**
	 * main 說明：程式進入點<br>
	 * 
	 * @author Alan Hsu
	 */
	public static void main(String[] args) {

		String startStr = getCurrentTimeStr();
		
		System.out.println( "### Codegen process start at " + startStr + " ###");
		System.out.println( "CustomCodegen Version: " + CODEGEN_VERSION );
		try {
			
			/**
			CustomCodegen gen = new CustomCodegen();
			gen.sample_one( gen );
			*/
			
			runCodegen();
			
		} catch( Exception e ) {
			System.err.println( "### Codegen execute runCodegen failed due to " + e.getMessage() + "###");
			e.printStackTrace();
		}
		
		System.out.print( System.lineSeparator() );
		
		String endStr = getCurrentTimeStr();
		
		System.out.println( "### Codegen process end at " + endStr + " ###");
	}

	/**
	 * excute 說明：產生Bean & IDao & Dao 檔案<br>
	 * 
	 * @author Alan Hsu
	 * @throws Exception 
	 */
	public void excute() throws Exception  {

		String nowStr = getCurrentTimeStr();
		setCurrentTime( nowStr );
		
		if( !getTableInfo() ) {
			System.err.println( "### Codegen process terminated due to exception ###");
			return;
		}
		
		System.out.println( tableInfo );
		
		generateBean();
		
		if( getNeedDao() ) {
			generateIDao();
			generateDao();
		}
		System.out.println("### Codegen Process Finished ###");
	}

	/**
	 * checkRequiredSetting 說明：檢核設定<br>
	 * 
	 * @author Alan Hsu
	 * @return boolean valid 
	 */
	public boolean checkRequiredSetting() {
		StringBuilder sb = new StringBuilder("");
		
		if( "".equals( toCleanString( getDomainObjectName() ) ) ) {
			sb.append("[檔案名稱(DomainObjectName)]" + System.lineSeparator() );
		}
		if( "".equals( toCleanString( getTableName() ) ) ) {
			sb.append("[表格名稱(TableName)]" + System.lineSeparator() );
		}
		if( "".equals( toCleanString( getTargetBeanPath() ) ) ) {
			sb.append("[目標Entity路徑(TargetBeanPath)]" + System.lineSeparator() );
		}
		if( getNeedDao() ) {
			if( "".equals( toCleanString( getTargetDaoPath() ) ) ) {
				sb.append("[目標Dao路徑(TargetDaoPath)]" + System.lineSeparator() );
			}
			if( "".equals( toCleanString( getTargetIDaoPath() ) ) ) {
				sb.append("[目標IDao路徑(TargetIDaoPath)]" + System.lineSeparator() );
			}
		}
		if( !"".equals( sb.toString() ) ) {
			sb.append("必須設定!!!");
		}
		if( !"".equals( sb.toString() ) ) {
			System.err.println( sb.toString() );
			return false;
		} else {
			return true;
		}
	}

	/**
	 *	getConnection 說明：取得對應的DB連線<br>
	 *	@author Alan Hsu
	 */
	public Connection getConnection() {
		Connection conn = null;
		try {
			String db = getDbName();
			if( "MySQL".equals( db ) ) {
				conn = getMySQLConnection();
				System.out.println("### MySQL Connection ###");
			} else {
				conn = getOracleConnection();
				System.out.println("### Oracle Connection ###");
			}
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return conn;
	}

	/**
	 * generateBean 說明：產生Bean檔案<br>
	 * 
	 * @author Alan Hsu
	 */
	public void generateBean() {
		try {
			
			if( tableInfo == null ) {
				System.err.println("無表格資訊");
				return;
			}
			
			String beanName = getDomainObjectName() + "Entity";
			setDomainBeanName( beanName );
			
			String newPathStr = getNewFilePath( "bean" );
			//System.out.println("newPath =" + newPathStr );
			
			List<String> lines = new ArrayList<>();
			lines.add( getPackageStr( "bean" ) );
			String beanImport = getBeanImportStr();
			if( !"".equals( beanImport ) ) {
				lines.add( beanImport );
			}
			lines.add( getBasicBeanImportStr() );
			lines.add( getClassInfoBlockStr( "bean" ) );
			lines.add( getBeanClassStartStr( beanName ) );
			
			String fieldsSpace = "\t";
			
			Map<String,ColumnInfo> column = tableInfo.getColumns();
			
			StringBuilder fields = new StringBuilder( fieldsSpace + System.lineSeparator() );
			//for( Map.Entry<String, ColumnInfo> entry : column.entrySet() )
			for( Map.Entry<String, ColumnInfo> entry : column.entrySet() ) {
				
				String columnName = entry.getKey();
				ColumnInfo columnData = entry.getValue();
				
				String cColumnName = "";
				if( getColumns() != null && !"".equals( getColumns() ) ) {
					cColumnName = columnData.getSelfColumnName();
				} else {
					cColumnName = convertCase( columnName, "camel" );
				}
				
				fields.append( getFieldDescription( columnName, columnData.getRemarks() ) );
				if( columnData.isPrimaryKey() ) {
					fields.append( fieldsSpace + "@Id" + System.lineSeparator() );
					if( "@Column".equals( getMappedType() ) ) {
						fields.append( fieldsSpace + "@GeneratedValue(strategy=GenerationType.IDENTITY)" + System.lineSeparator() );
						fields.append( fieldsSpace + "@Column(name=\"" + columnName + "\")" + System.lineSeparator() );
					}
					fields.append( fieldsSpace + "private " + columnData.getJavaType() + " " + cColumnName + ';' + System.lineSeparator() );
					fields.append( fieldsSpace + System.lineSeparator() );
				} else {
					if( "@Column".equals( getMappedType() ) ) {
						fields.append( fieldsSpace + "@Column(name=\"" + columnName + "\")" + System.lineSeparator() );
					} else {
						fields.append( fieldsSpace + "@Basic" + System.lineSeparator() );
					}
					fields.append( fieldsSpace + "private " + columnData.getJavaType() + " " + cColumnName + ';' + System.lineSeparator() );
					fields.append( fieldsSpace + System.lineSeparator() );
				}
			}
			lines.add( fields.toString() );
			fields = null;
			
			StringBuilder methods = new StringBuilder( fieldsSpace + System.lineSeparator()  );
			for( Map.Entry<String, ColumnInfo> entry : column.entrySet() ) {
				
				String columnName = entry.getKey();
				ColumnInfo columnData = entry.getValue();
				
				String cColumnName = "";
				if( getColumns() != null && !"".equals( getColumns() ) ) {
					cColumnName = columnData.getSelfColumnName();
				} else {
					cColumnName = convertCase( columnName, "camel" );
				}
				String pColumnName = "";
				if( getColumns() != null && !"".equals( getColumns() ) ) {
					pColumnName = convertCase( columnData.getSelfColumnName(), "capitalizeOnlyFirst" );
				} else {
					pColumnName = convertCase( columnName, "pascal" );
				}
				
				if( columnData.isPrimaryKey() ) {
					methods.append( getMethodGetterDescription( columnName ) );
					methods.append( fieldsSpace + "public " + columnData.getJavaType() + " get" + pColumnName + "() {" + System.lineSeparator() );
					methods.append( fieldsSpace + fieldsSpace + "return " + cColumnName + ';' + System.lineSeparator() );
					methods.append( fieldsSpace + '}' + System.lineSeparator() );
					methods.append( fieldsSpace + System.lineSeparator() );
					methods.append( getMethodSetterDescription( columnName ) );
					methods.append( fieldsSpace + "public void set" + pColumnName + "( ");
					methods.append( columnData.getJavaType() + " " + cColumnName + " ) {" + System.lineSeparator() );
					methods.append( fieldsSpace + fieldsSpace + "this." + cColumnName + " = " + cColumnName + ';' + System.lineSeparator() );
					methods.append( fieldsSpace + '}' + System.lineSeparator() );
					methods.append( fieldsSpace + System.lineSeparator() );
				} else {
					methods.append( getMethodGetterDescription( columnName ) );
					methods.append( fieldsSpace + "public " + columnData.getJavaType() + " get" + pColumnName + "() {" + System.lineSeparator() );
					methods.append( fieldsSpace + fieldsSpace + "return " + cColumnName + ';' + System.lineSeparator() );
					methods.append( fieldsSpace + '}' + System.lineSeparator() );
					methods.append( fieldsSpace + System.lineSeparator() );
					methods.append( getMethodSetterDescription( columnName ) );
					methods.append( fieldsSpace + "public void set" + pColumnName + "( ");
					methods.append( columnData.getJavaType() + " " + cColumnName + " ) {" + System.lineSeparator() );
					methods.append( fieldsSpace + fieldsSpace + "this." + cColumnName + " = " + cColumnName + ';' + System.lineSeparator() );
					methods.append( fieldsSpace + '}' + System.lineSeparator() );
					methods.append( fieldsSpace + System.lineSeparator() );
				}
			}
			lines.add( methods.toString() );
			methods = null;
			
			if( getNeedToString() ) {
				String doubleFieldsSpace = "\t\t";
				StringBuilder toStringSB = new StringBuilder( fieldsSpace + System.lineSeparator()  );
				toStringSB.append( fieldsSpace + "@Override" + System.lineSeparator() );
				toStringSB.append( fieldsSpace + "public String toString() {" + System.lineSeparator() );
				toStringSB.append( doubleFieldsSpace + "return this.getClass().getSimpleName() + " );
				String lineSepStr = "System.lineSeparator()";
				toStringSB.append( "\" [\" + " + lineSepStr + " + " );
				for( Map.Entry<String, ColumnInfo> entry : column.entrySet() ) {
					toStringSB.append( System.lineSeparator() + doubleFieldsSpace + fieldsSpace );
					ColumnInfo columnData = entry.getValue();
					
					String cColumnName = "";
					if( getColumns() != null && !"".equals( getColumns() ) ) {
						cColumnName = columnData.getSelfColumnName();
					} else {
						cColumnName = convertCase( columnData.getColumnName(), "camel" );
					}
					String remarks = columnData.getRemarks() == null ? "" : '(' + columnData.getRemarks() + ')';
					toStringSB.append( "\"" + cColumnName + remarks + ':' + "\" + " + cColumnName + " + ',' + " );
					toStringSB.append( lineSepStr + " + " );
				}
				toStringSB.append( System.lineSeparator() );
				toStringSB.append( doubleFieldsSpace + fieldsSpace + "']';" );
				toStringSB.append( System.lineSeparator() );
				toStringSB.append( fieldsSpace + '}' + System.lineSeparator() );
				lines.add( toStringSB.toString() );
			}
			
			lines.add( getClassEndStr() );
			
			Path newPath = Paths.get( newPathStr );
			if( !Files.exists( newPath) ) {
				Files.createDirectories( newPath );
				System.out.println("Directory created." );
			} else {
				System.out.println("Directory already exists." );
			}
			
			String newFilePath = newPathStr + File.separator + beanName + ".java";
			Path file = Paths.get( newFilePath );
			System.out.println("New Entity Path =" + newFilePath );
			Files.write( file, lines, StandardCharsets.UTF_8 );
			System.out.println( "[ " + beanName + ".java ] generated successful." + System.lineSeparator()  );
			
		} catch( IOException e ) {
			e.printStackTrace();
		}

	}
	
	/**
	 * generateIDao 說明：產生IDao檔案<br>
	 * 
	 * @author Alan Hsu
	 */
	public void generateIDao() {
		try {
			
			if( tableInfo == null ) {
				System.err.println("無表格資訊");
				return;
			}
			
			String iDaoName = "I" + getDomainObjectName() + "Dao";
			setDomainIDaoName( iDaoName );
			
			String newPathStr = getNewFilePath( "IDao" );
			
			List<String> lines = new ArrayList<>();
			lines.add( getPackageStr( "IDao" ) );
			lines.add( getBasicIDaoImportStr() );
			lines.add( getClassInfoBlockStr( "IDao" ) );
			lines.add( getIDaoClassStartStr( iDaoName ) );
			lines.add( getClassEndStr() );
			
			Path newPath = Paths.get( newPathStr );
			if( !Files.exists( newPath) ) {
				Files.createDirectories( newPath );
				System.out.println("Directory created." );
			} else {
				System.out.println("Directory already exists." );
			}
			
			String newFilePath = newPathStr + File.separator + iDaoName + ".java";
			Path file = Paths.get( newFilePath );
			System.out.println("New IDao Path =" + newFilePath );
			Files.write( file, lines, StandardCharsets.UTF_8 );
			System.out.println( "[ " + iDaoName + ".java ] generated successful." + System.lineSeparator()  );
			
		} catch( IOException e ) {
			e.printStackTrace();
		}

	}
	
	/**
	 * generateDao 說明：產生Dao檔案<br>
	 * 
	 * @author Alan Hsu
	 */
	public void generateDao() {
		try {
			
			if( tableInfo == null ) {
				System.err.println("無表格資訊");
				return;
			}
			
			String daoName = getDomainObjectName() + "Dao";
			setDomainDaoName( daoName );
			
			String newPathStr = getNewFilePath( "dao" );
			
			List<String> lines = new ArrayList<>();
			lines.add( getPackageStr( "dao" ) );
			lines.add( getBasicDaoImportStr() );
			lines.add( getClassInfoBlockStr( "dao" ) );
			lines.add( getDaoClassStartStr( daoName ) );
			lines.add( getClassEndStr() );
			
			Path newPath = Paths.get( newPathStr );
			if( !Files.exists( newPath) ) {
				Files.createDirectories( newPath );
				System.out.println("Directory created." );
			} else {
				System.out.println("Directory already exists." );
			}
			
			String newFilePath = newPathStr + File.separator + daoName + ".java";
			Path file = Paths.get( newFilePath );
			System.out.println("New Dao Path =" + newFilePath );
			Files.write( file, lines, StandardCharsets.UTF_8 );
			System.out.println( "[ " + daoName + ".java ] generated successful." + System.lineSeparator()  );
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * getBasicImportStr 說明：取得套件路徑字串<br>
	 * 
	 * @return String packageStr
	 * @author Alan Hsu
	 */
	public String getPackageStr( String type ) {
		type = type.toLowerCase();
		String str = "";
		if( "bean".equals( type ) ) {
			str = getTargetBeanPackage();
		} else if( "dao".equals( type ) ) {
			str = getTargetDaoPackage();
		} else if( "idao".equals( type ) ) {
			str = getTargetIDaoPackage();
		}
		return "package " + str + ';' + System.lineSeparator() ;
	}

	/**
	 * getBasicBeanImportStr 說明：取得Bean必要引用字串<br>
	 * 
	 * @return String basicBeanImportStr
	 * @author Alan Hsu
	 */
	public String getBasicBeanImportStr() {
		StringBuilder sb = new StringBuilder("");
		
		if( "@Column".equals( getMappedType() ) ) {
			sb.append("import javax.persistence.Column;" + System.lineSeparator() );
		} else {
			sb.append("import javax.persistence.Basic;" + System.lineSeparator() );
		}
		sb.append("import javax.persistence.Entity;" + System.lineSeparator() );
		if( tableInfo.getHasPrimaryKey() ) {
			if( "@Column".equals( getMappedType() ) ) {
				sb.append("import javax.persistence.GeneratedValue;" + System.lineSeparator() );
				sb.append("import javax.persistence.GenerationType;" + System.lineSeparator() );
			}
			sb.append("import javax.persistence.Id;" + System.lineSeparator() );
		}
		sb.append("import javax.persistence.Table;" + System.lineSeparator() );
		return sb.toString();
	}

	/**
	 * getBasicIDaoImportStr 說明：取得IDao必要引用字串<br>
	 * 
	 * @return String basicIDaoImportStr
	 * @author Alan Hsu
	 */
	public String getBasicIDaoImportStr() {
		StringBuilder sb = new StringBuilder( System.lineSeparator() );
		/**
		if( "highSchool".equals( getDbName() ) ) {
			if( !getTargetIDaoPackage().contains(".highSchool.dao") ) {
				sb.append( "import com.tw.persistence.highSchool.dao.ISCHDao;" ).append( System.lineSeparator() );
			}
		} else {
			if( !getTargetIDaoPackage().contains(".college.dao") ) {
				sb.append( "import com.tw.persistence.college.dao.ICLGDao;" ).append( System.lineSeparator() );
			}
		}
		*/
		sb.append( "import " ).append( getTargetBeanPackage() ).append( '.' ).append( getDomainBeanName() ).append( ';' ).append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 *	getBasicDaoImportStr 說明：取得Dao必要引用字串<br>
	 *	@return String basicDaoImportStr
	 *	@author Alan Hsu
	 */
	public String getBasicDaoImportStr() {
		StringBuilder sb = new StringBuilder( "" + System.lineSeparator() );
		/**
		if( "highSchool".equals( getDbName() ) ) {
			if( !getTargetInterfacePackage().contains( ".highSchool.dao" ) ) {
				sb.append( "import com.tw.persistence.highSchool.dao.ISCHDao;" ).append( System.lineSeparator() );
			}
		} else {
			if( !getTargetInterfacePackage().contains( ".college.dao" ) ) {
				sb.append( "import com.tw.persistence.college.dao.ICLGDao;" ).append( System.lineSeparator() );
			}
		}
		*/
		sb.append( "import " ).append( getTargetIDaoPackage() ).append( '.' ).append( getDomainIDaoName() ).append( ';' ).append( System.lineSeparator() );
		sb.append( "import " ).append( getTargetBeanPackage() ).append( '.' ).append( getDomainBeanName() ).append( ';' ).append( System.lineSeparator() );
		return sb.toString();
	}
	
	/**
	 * getClassInfoBlockStr 說明：取得程式說明區塊字串<br>
	 * 
	 * @param String type
	 * @return String classStartStr
	 * @author Alan Hsu
	 */
	public String getClassInfoBlockStr( String type ) {
		StringBuilder sb = new StringBuilder("");
		sb.append("/**" + System.lineSeparator() );
		sb.append(" *	作 業 代 碼 ：" + toCleanString( getTaskId() ) + "<br>" + System.lineSeparator() );
		sb.append(" *	作 業 名 稱 ：" + toCleanString( getTaskName() ) + "<br>" + System.lineSeparator() );
		sb.append(" *	程 式 代 號 ：" + getFileNameByType( type ) + ".java<br>" + System.lineSeparator() );
		sb.append(" *	描	   述 ：" + toCleanString( getTaskDescription() ) + "<br>" + System.lineSeparator() );
		sb.append(" *	公	   司 ：Tenpastten Studio<br>" + System.lineSeparator() );
		sb.append(" *	【 資 料 來 源】 ：" + getSourceDescription() + "<br>" + System.lineSeparator() );
		sb.append(" *	【 異 動 紀 錄】 ：<br>" + System.lineSeparator() );
		sb.append(" *" + System.lineSeparator() );
		sb.append(" *	@author : " + author + "<br>" + System.lineSeparator() );
		sb.append(" *	@version : 1.0.0  " + getCurrentTime() + "<br>" + System.lineSeparator() );
		sb.append(" */");
		return sb.toString();
	}

	/**
	 * getBeanClassStartStr 說明：取得Bean新檔案class名稱<br>
	 * 
	 * @param String entityName
	 * @return String classStartStr
	 * @author Alan Hsu
	 */
	public String getBeanClassStartStr( String entityName ) {
		StringBuilder sb = new StringBuilder("");
		sb.append("@Entity");
		sb.append( System.lineSeparator() );
		sb.append("@Table(name=\"" + getTableName().toUpperCase() + "\")");
		sb.append( System.lineSeparator() );
		sb.append("public class " + entityName + " {" );
		sb.append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 * getIDaoClassStartStr 說明：取得IDao新檔案class名稱<br>
	 * 
	 * @param String entityName
	 * @return String classStartStr
	 * @author Alan Hsu
	 */
	public String getIDaoClassStartStr( String entityName ) {
		StringBuilder sb = new StringBuilder("");
		sb.append( "public interface " ).append( entityName ).append( " " );
		/**
		if( "highSchool".equals( getDbName() ) ) {
			sb.append( "extends ISCHDao<" + getDomainBeanName() + "> ");
		} else {
			sb.append( "extends ICOLDao<" + getDomainBeanName() + "> ");
		}
		*/
		sb.append( '{' ).append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 * getDaoClassStartStr 說明：取得Dao新檔案class名稱<br>
	 * 
	 * @param String entityName
	 * @return String classStartStr
	 * @author Alan Hsu
	 */
	public String getDaoClassStartStr( String entityName ) {
		StringBuilder sb = new StringBuilder("");
		sb.append( "public class " ).append( entityName ).append( " ");
		/**
		if( "highSchool".equals( getDbName() ) ) {
			sb.append("extends SCHDao<" + getDomainBeanName() + "> ");
		} else {
			sb.append("extends COLDao<" + getDomainBeanName() + "> ");
		}
		*/
		sb.append( "implements " ).append( getDomainIDaoName() ).append( " " );
		sb.append( '{' ).append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 * getClassEndStr 說明：取得產檔class的結尾大括號<br>
	 * 
	 * @return String classEndStr
	 * @author Alan Hsu
	 */
	public static String getClassEndStr() {
		StringBuilder sb = new StringBuilder("");
		sb.append( '}' ).append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 * getBeanImportStr 說明：取得Bean需要引用的其他型別<br>
	 * 
	 * @return String importStr
	 * @author Alan Hsu
	 */
	public String getBeanImportStr() {
		StringBuilder sb = new StringBuilder("");
		if( getHasBigDecimal() ) {
			sb.append("import java.math.BigDecimal;" + System.lineSeparator() );
		}
		if( getHasTimeStamp() ) {
			sb.append("import java.sql.Timestamp;" + System.lineSeparator() );
		}
		if( getHasDate() ) {
			sb.append("import java.util.Date;" + System.lineSeparator() );
		}
		return sb.toString();
	}

	/**
	 * setColumnTypeImport 說明：設定是否需要引用其他型別之旗標<br>
	 * 
	 * @param String columnTypeName
	 * @author Alan Hsu
	 */
	public void checkColumnTypeImport( String columnTypeName ) {
		
		switch ( columnTypeName.toUpperCase() ) {
			case "BIGDECIMAL":setHasBigDecimal( true );break;
			case "DATE":setHasDate( true );break;
			case "TIMESTAMP":setHasTimeStamp( true );break;
		}
	}

	/**
	 * getFieldDescription 說明：取得成員變數說明<br>
	 * 
	 * @return String importStr
	 * @author Alan Hsu
	 */
	public String getFieldDescription( String columnsName, String remarks ) {
		String fieldsSpace = "\t";
		StringBuilder sb = new StringBuilder( "" );
		sb.append( fieldsSpace + "/**" + System.lineSeparator() );
		sb.append( fieldsSpace + " * This field was generated by CustomCodegen Generator. ");
		sb.append( "This field corresponds to the database column " + getTableName().toUpperCase() );
		sb.append( '.' + columnsName + "" + System.lineSeparator() );
		sb.append( fieldsSpace + " * Remarks: " + ( remarks == null ? "NONE" : remarks )+ System.lineSeparator() );
		sb.append( fieldsSpace + " * cusg.generated  " + getCurrentTime() + System.lineSeparator() );
		sb.append( fieldsSpace + " */" + System.lineSeparator() );
		return sb.toString();
	}

	/**
	 * getMethodSetterDescription 說明：取得設值方法說明<br>
	 * 
	 * @return String str
	 * @author Alan Hsu
	 */
	public String getMethodSetterDescription( String columnsName ) {
		String fieldsSpace = "\t";
		String correspondsCol = getTableName().toUpperCase() + '.' + columnsName;
		StringBuilder sb = new StringBuilder( "" );
		sb.append( fieldsSpace + "/**" + System.lineSeparator() );
		sb.append( fieldsSpace + " * This method was generated by CustomCodegen Generator. ");
		sb.append( "This method sets the value of the database column " + correspondsCol + System.lineSeparator() );
		sb.append( fieldsSpace + " * @param " + toCamelCase( columnsName ) );
		sb.append( "  the value for " + correspondsCol + System.lineSeparator() );
		sb.append( fieldsSpace + " * cusg.generated  " + getCurrentTime() + System.lineSeparator() );
		sb.append( fieldsSpace + " */" + System.lineSeparator() );
		return sb.toString();
	}

	/**
	 * getMethodGetterDescription 說明：取得取值方法說明<br>
	 * 
	 * @return String str
	 * @author Alan Hsu
	 */
	public String getMethodGetterDescription( String columnsName ) {
		String fieldsSpace = "\t";
		String correspondsCol = getTableName().toUpperCase() + '.' + columnsName;
		StringBuilder sb = new StringBuilder( "" );
		sb.append( fieldsSpace + "/**" + System.lineSeparator() );
		sb.append( fieldsSpace + " * This method was generated by CustomCodegen Generator. ");
		sb.append( "This method returns the value of the database column " + correspondsCol + System.lineSeparator() );
		sb.append( fieldsSpace + " * @return " + toCamelCase( columnsName ) );
		sb.append( "  the value of " + correspondsCol + System.lineSeparator() );
		sb.append( fieldsSpace + " * cusg.generated  " + getCurrentTime() + System.lineSeparator() );
		sb.append( fieldsSpace + " */" + System.lineSeparator() );
		return sb.toString();
	}

	/**
	 * getNewFilePath 說明：取得新檔案路徑<br>
	 * 
	 * @param String type
	 * @return String newFilePath
	 * @author Alan Hsu
	 */
	public String getNewFilePath( String type ) {
		
		type = type.toLowerCase();
		
		String sep = File.separator;
		String path = "";
		String packagePath = "";
		
		if( "".equals( type ) ) {
			/** Working Directory */
			String directory = Paths.get(".").toAbsolutePath().normalize().toString();
			
			/** Package */
			packagePath = CustomCodegen.class.getPackage().getName();
			setTargetBeanPackage( packagePath );
			
			String packageStr = packagePath.replaceAll( "\\.", Matcher.quoteReplacement( sep ) );
			
			path = directory + sep + "src" + sep + packageStr;
		} else if( "bean".equals( type ) ) {
			String directory = getTargetBeanPath();
			directory = directory.replaceAll( "\\.", Matcher.quoteReplacement( sep ) );
			path = directory;
			/** bean Package */
			int srcIdx = directory.indexOf("src");
			if( srcIdx > 0 ) {
				String srcStr = directory.substring( srcIdx + 4 );
				packagePath = srcStr.replaceAll( "\\\\", "." );
			} else {
				packagePath = directory.replaceAll( "\\\\", "." );
			}
			if( packagePath.endsWith(".") ) {
				packagePath = packagePath.substring( 0, packagePath.length() -1 );
			}
			setTargetBeanPackage( packagePath );
		} else if( "idao".equals( type ) ) {
			String directory = getTargetIDaoPath();
			directory = directory.replaceAll( "\\.", Matcher.quoteReplacement( sep ) );
			path = directory;
			/** idao Package */
			int srcIdx = directory.indexOf("src");
			if( srcIdx > 0 ) {
				String srcStr = directory.substring( srcIdx + 4 );
				packagePath = srcStr.replaceAll( "\\\\", "." );
			} else {
				packagePath = directory.replaceAll( "\\\\", "." );
			}
			if( packagePath.endsWith(".") ) {
				packagePath = packagePath.substring( 0, packagePath.length() -1 );
			}
			setTargetIDaoPackage( packagePath );
		} else if( "dao".equals( type ) ) {
			String directory = getTargetDaoPath();
			directory = directory.replaceAll( "\\.", Matcher.quoteReplacement( sep ) );
			path = directory;
			/** dao Package */
			int srcIdx = directory.indexOf("src");
			if( srcIdx > 0 ) {
				String srcStr = directory.substring( srcIdx + 4 );
				packagePath = srcStr.replaceAll( "\\\\", "." );
			} else {
				packagePath = directory.replaceAll( "\\\\", "." );
			}
			if( packagePath.endsWith(".") ) {
				packagePath = packagePath.substring( 0, packagePath.length() -1 );
			}
			setTargetDaoPackage( packagePath );
		}
		/** 產出檔案輸出位置和package位置脫鉤 2022-08-16 */
		path = getNewFileTargetFolder();
		System.out.println("packagePath =" + packagePath );
		System.out.println("output path =" + path );
		return path;
	}

	/**
	 * getFileNameByType 說明：取得對應的檔案名稱<br>
	 * 
	 * @param String type
	 * @return String fileName
	 * @author Alan Hsu
	 */
	public String getFileNameByType( String type ) {
		type = type.toLowerCase();
		String fileName = "";
		if( "bean".equals( type ) ) {
			fileName = getDomainBeanName();
		} else if( "dao".equals( type ) ) {
			fileName = getDomainDaoName();
		} else if( "idao".equals( type ) ) {
			fileName = getDomainIDaoName();
		}
		return fileName;
	}

	/**
	 * getTableInfo 說明：取得表格資訊<br>
	 * 
	 * @return boolean tableInfoExists
	 * @author Alan Hsu
	 */
	public boolean getTableInfo() throws Exception {
		
		TableInfo tableInfo = new TableInfo( getTableName() );
		
		try( Connection conn = getConnection() ) {
			
			Statement stmt = conn.createStatement();
			
			boolean useSelfColumn = false;
			
			String columns = getColumns();
			List<String> selfList = null;
			if( columns == null || "".equals( columns ) || columns.contains("*") ) {
				columns = "*";
			} else {
				useSelfColumn = true;
				/**
				String[] selfAry = columns.trim().split(",");
				selfList = new ArrayList<>();
				for( String s : selfAry ) {
					selfList.add( s.trim() );
				}
				*/
				selfList = Arrays.asList( columns.trim().split(",") )
						.stream().map( s -> s.trim() ).collect( Collectors.toList() );
			}
			String sql = "SELECT " + columns + " FROM " + getTableName() + " WHERE 1 = 2";
			System.out.println( sql );
			
			Map<String,ColumnInfo> columnMap = new LinkedHashMap<>();
			
			try ( ResultSet rs = stmt.executeQuery( sql ) ) {
			
				ResultSetMetaData rsmd = rs.getMetaData();
				/** DatabaseMetaData dbmd = conn.getMetaData(); */
				
				int columnCount = rsmd.getColumnCount();
				tableInfo.setColumnCount( columnCount );
				
				for( int i = 1; i <= rsmd.getColumnCount(); i++ ) {
					ColumnInfo columnInfo = new ColumnInfo();
					String columnName = rsmd.getColumnName( i );
					columnInfo.setColumnName( columnName );
					String columnType = rsmd.getColumnTypeName( i );
					columnInfo.setColumnType( columnType );
					String columnClassName = rsmd.getColumnClassName( i );
					
					int lastComma = columnClassName.lastIndexOf('.');
					String javaType = columnClassName.substring( lastComma + 1 );
					/**
					 * System.out.println( columnName + ", columnType:" + columnType + ", javaType:" + columnClassName );
					 */
					columnInfo.setJavaType( javaType );
					checkColumnTypeImport( javaType );
					
					/** 使用使用者提供的欄位名稱及大小寫 */
					if( useSelfColumn ) {
						String selfColumn = selfList.get( i - 1 );
						columnInfo.setSelfColumnName( selfColumn );
					}
					
					columnMap.put( columnInfo.getColumnName(), columnInfo );
				}
			} catch( Exception e ) {
				System.err.println( "### Statement.executeQuery occur exception ###");
				throw e;
			}
			
			/**To Get Column PK*/
			try ( ResultSet rs = conn.getMetaData()
					.getPrimaryKeys( null, null, tableName ) ) {
				while ( rs.next() ) {
					String pKey = rs.getString("COLUMN_NAME");
					columnMap.get( pKey ).setPrimaryKey( true );
					tableInfo.setHasPrimaryKey( true );
				}
			} catch( Exception e ) {
				System.err.println( "### DatabaseMetaData.getPrimaryKeys occur exception ###");
				throw e;
			}
			
			/**To Get Column Remarks*/
			try ( ResultSet rs = conn.getMetaData()
					.getColumns( null, null, getTableName().toUpperCase(), null ) ) {
				while ( rs.next() ) {
					String remarks = rs.getString("REMARKS");
					String columnName = rs.getString("COLUMN_NAME");
					
					if( columnMap.get( columnName ) != null ) {
						columnMap.get( columnName ).setRemarks( remarks );
					}
				}
			} catch( Exception e ) {
				System.err.println( "### DatabaseMetaData.getColumns occur exception ###");
				throw e;
			}
			
			tableInfo.setColumns( columnMap );
			setTableInfo( tableInfo );
			
		} catch( SQLSyntaxErrorException e ) {
			System.err.println( "### Query table info failed due to invalid SQL field or table name ###");
			e.printStackTrace();
			return false;
		} catch( Exception e ) {
			e.printStackTrace();
			return false;
		} finally {
			System.out.println("### Connection closed ###");
			System.out.print( System.lineSeparator() );
		}
		return true;
	}

	/**
	 *	getOracleConnection 說明：取得Oracle DB連線<br>
	 *	@return Connection
	 *	@author Alan Hsu
	 */
	public Connection getOracleConnection() throws Exception {
		/*
		 * Build Path > Config Build Path > Libraries > Add External Jars
		 * ojdbc8.jar
		 */
		Class.forName("oracle.jdbc.driver.OracleDriver");
		return DriverManager.getConnection( Oracle_SERVER_URL, Oracle_ACCOUNT, Oracle_PWD );
	}

	/**
	 *	getMySQLConnection 說明：取得MySQL DB連線<br>
	 *	@return Connection
	 *	@author Alan Hsu
	 */
	public Connection getMySQLConnection() throws Exception {
		Class.forName("com.mysql.cj.jdbc.Driver");
		return DriverManager.getConnection( MySQL_SERVER_URL, MySQL_ACCOUNT, MySQL_PWD );
	}

	/**
	 * convertCase 說明：依據命名規範設定,調整字串<br>
	 * 
	 * @param String txt
	 * @return String result
	 * @author Alan Hsu
	 */
	public String convertCase( String txt ) {
		String naming = toCleanString( getNamingConventions() );
		String result = "";
		switch ( naming ) {
			case "camal":
				result = toCamelCase( txt );
				break;
			case "pascal":
				result = toPascalCase( txt );
				break;
			case "snake":
				result = toSnakeCase( txt );
				break;
			case "capitalize":
				result = capitalizeFirst( txt );
				break;
			default:
				result = toCamelCase( txt );
				break;
			}
		return result;
	}

	/**
	 * convertCase 說明：依據命名規範設定,調整字串<br>
	 * 
	 * @param String txt
	 * @return String result
	 * @author Alan Hsu
	 */
	public String convertCase( String txt, String naming ) {
		String result = "";
		switch ( naming ) {
			case "camal":
				result = toCamelCase( txt );
				break;
			case "pascal":
				result = toPascalCase( txt );
				break;
			case "snake":
				result = toSnakeCase( txt );
				break;
			case "capitalize":
				result = capitalizeFirst( txt );
				break;
			case "capitalizeOnlyFirst":
				result = capitalizeOnlyFirst( txt );
				break;
			default:
				result = toCamelCase( txt );
				break;
			}
		return result;
	}

	/**
	 * toCamelCase 說明：欄位資訊字串轉駝峰命名(E.g. camelCase)<br>
	 * 
	 * @param String text
	 * @return String camel
	 * @author Alan Hsu
	 */
	public String toCamelCase( String text ) {
		String[] words = text.split("[\\W_]+");
		
		StringBuilder camel = new StringBuilder();
		for( int i = 0; i < words.length; i++ ) {
			String word = words[i];
			if( i == 0 ) {
				word = word.isEmpty() ? word : word.toLowerCase();
			} else {
				word = word.isEmpty() ? word : Character.toUpperCase( word.charAt(0) ) + word.substring(1).toLowerCase();
			}
			camel.append( word );
		}
		return camel.toString();
	}

	/**
	 * toPascalCase 說明：欄位資訊字串轉帕斯卡命名(E.g. PascalCase)<br>
	 * 
	 * @param String text
	 * @return String pascal
	 * @author Alan Hsu
	 */
	public String toPascalCase( String text ) {
		String[] words = text.split("[\\W_]+");
		
		StringBuilder pascal = new StringBuilder();
		for( int i = 0; i < words.length; i++ ) {
			String word = words[i];
			if( i == 0 ) {
				word = word.isEmpty() ? word : capitalizeFirst( word );
			} else {
				word = word.isEmpty() ? word : Character.toUpperCase( word.charAt(0) ) + word.substring(1).toLowerCase();
			}
			pascal.append( word );
		}
		return pascal.toString();
	}

	/**
	 * toSnakeCase 說明：欄位資訊字串轉蛇形命名(E.g. Snake_Case)<br>
	 * 
	 * @param String text
	 * @return String text
	 * @author Alan Hsu
	 */
	public String toSnakeCase( String text ) {
		return text.toLowerCase();
	}

	/**
	 * capitalizeFirst 說明：字首轉大寫,其餘轉小寫<br>
	 * 
	 * @param String word
	 * @return String word
	 * @author Alan Hsu
	 */
	private String capitalizeFirst( String word ) {
		return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
	}

	/**
	 * capitalizeOnlyFirst 說明：只將字首轉大寫<br>
	 * 
	 * @param String word
	 * @return String word
	 * @author Alan Hsu
	 */
	private String capitalizeOnlyFirst( String word ) {
		return word.substring(0, 1).toUpperCase() + word.substring(1);
	}

	/**
	 * toCleanString 說明：將參數字串null轉為空字串並且去空白<br>
	 * @param Object paramObj
	 * @return String strVal
	 * @author Alan Hsu
	 */
	public String toCleanString( Object paramObj ) {
		String strVal = "";
		if( paramObj == null ) {
			strVal = "";
		} else {
			strVal = paramObj.toString().trim();
		}
		return strVal;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getTaskDescription() {
		return taskDescription;
	}

	public void setTaskDescription(String taskDescription) {
		this.taskDescription = taskDescription;
	}

	public String getSourceDescription() {
		return sourceDescription;
	}

	public void setSourceDescription(String sourceDescription) {
		this.sourceDescription = sourceDescription;
	}

	public String getDomainObjectName() {
		return domainObjectName;
	}

	public void setDomainObjectName(String domainObjectName) {
		this.domainObjectName = domainObjectName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getTableSchema() {
		return tableSchema;
	}

	public void setTableSchema(String tableSchema) {
		this.tableSchema = tableSchema;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getColumns() {
		return columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	public String getDbName() {
		String dbNameStr = dbName == null ? "" : dbName.toUpperCase();
		return dbNameStr;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public void setTableInfo(TableInfo tableInfo) {
		this.tableInfo = tableInfo;
	}

	public String getNamingConventions() {
		return namingConventions;
	}

	public void setNamingConventions(String namingConventions) {
		this.namingConventions = namingConventions;
	}

	public String getDomainBeanName() {
		return domainBeanName;
	}

	public void setDomainBeanName(String domainBeanName) {
		this.domainBeanName = domainBeanName;
	}

	public String getDomainDaoName() {
		return domainDaoName;
	}

	public void setDomainDaoName(String domainDaoName) {
		this.domainDaoName = domainDaoName;
	}

	public String getDomainIDaoName() {
		return domainIDaoName;
	}

	public void setDomainIDaoName(String domainIDaoName) {
		this.domainIDaoName = domainIDaoName;
	}

	public String getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(String currentTime) {
		this.currentTime = currentTime;
	}

	public String getMappedType() {
		return mappedType;
	}

	public void setMappedType(String mappedType) {
		this.mappedType = mappedType;
	}
	
	public void setMappedTypeByCode(String code) {
		if( code == null || "1".equals( code ) ) {
			this.mappedType = "@Column";
		} else {
			this.mappedType = "@Basic";
		}
	}
	
	public void setMappedTypeByCode(Integer code) {
		if( code == null || code == 1 ) {
			this.mappedType = "@Column";
		} else {
			this.mappedType = "@Basic";
		}
	}

	public String getTargetBeanPackage() {
		return targetBeanPackage;
	}

    public void setTargetBeanPackage(String targetBeanPackage) {
		this.targetBeanPackage = targetBeanPackage;
	}

	public String getTargetDaoPackage() {
		return targetDaoPackage;
	}

	public void setTargetDaoPackage(String targetDaoPackage) {
		this.targetDaoPackage = targetDaoPackage;
	}

	public String getTargetIDaoPackage() {
		return targetIDaoPackage;
	}

	public void setTargetIDaoPackage(String targetIDaoPackage) {
		this.targetIDaoPackage = targetIDaoPackage;
	}

	public String getTargetBeanPath() {
		return targetBeanPath;
	}

	public void setTargetBeanPath(String targetBeanPath) {
		this.targetBeanPath = targetBeanPath;
	}

	public String getTargetDaoPath() {
		return targetDaoPath;
	}

	public void setTargetDaoPath(String targetDaoPath) {
		this.targetDaoPath = targetDaoPath;
	}

	public String getTargetIDaoPath() {
		return targetIDaoPath;
	}

	public void setTargetIDaoPath(String targetIDaoPath) {
		this.targetIDaoPath = targetIDaoPath;
	}

	public String getNewFileTargetFolder() {
		return newFileTargetFolder;
	}

	public void setNewFileTargetFolder(String newFileTargetFolder) {
		this.newFileTargetFolder = newFileTargetFolder;
	}

	public boolean getHasBigDecimal() {
		return hasBigDecimal;
	}

	public void setHasBigDecimal(boolean hasBigDecimal) {
		this.hasBigDecimal = hasBigDecimal;
	}

	public boolean getHasTimeStamp() {
		return hasTimeStamp;
	}

	public void setHasTimeStamp(boolean hasTimeStamp) {
		this.hasTimeStamp = hasTimeStamp;
	}

	public boolean getHasDate() {
		return hasDate;
	}

	public void setHasDate(boolean hasDate) {
		this.hasDate = hasDate;
	}

	public boolean getNeedDao() {
		return needDao;
	}
	
	public void setNeedDao(boolean needDao) {
		this.needDao = needDao;
	}
	
	public void setNeedDao(String str) {
		if( "y".equals( str ) || "Y".equals( str ) ) {
			this.needDao = true;
		} else {
			this.needDao = false;
		}
	}

	public boolean getNeedToString() {
		return needToString;
	}

	public void setNeedToString(boolean needToString) {
		this.needToString = needToString;
	}
	
	public void setNeedToString(String str) {
		if( "y".equals( str ) || "Y".equals( str ) ) {
			this.needToString = true;
		} else {
			this.needToString = false;
		}
	}
	
}

/**
 * Represents a predicate (boolean-valued function) of three argument.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #test(Object)}.
 *
 * @param <S> the type of the input to the predicate
 * @param <T> the type of the input to the predicate
 * @param <C> the type of the input to the predicate
 * 
 * @author Alan Hsu
 */
@FunctionalInterface
interface ThreeParameterPredicate<S, T, C, R> {
	public R test( S sc, T topic, C custom );
}
