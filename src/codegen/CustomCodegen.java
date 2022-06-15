package codegen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;

/**
 *	作 業 代 碼 ：CustomCodegen<br>
 *	作 業 名 稱 ：Custom Codegen for Special Framework<br>
 *	程 式 代 號 ：CustomCodegen.java<br>
 *	描 述 ：Custom Codegen Generator, 使用前須設定載入ojdbc.jar<br>
 *	公 司 ： Tenpastten Studio<br>
 *	【 資 料 來 源】 ：<br>
 *	【 異 動 紀 錄】 ：<br>
 *	@author : Alan Hsu<br>
 *	@version : 1.0.0 2022-05-10<br>
 */
public class CustomCodegen {

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

		public void setPrimaryKey(boolean primaryKey) {
			isPrimaryKey = primaryKey;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("");
			sb.append( "Field" ).append( ( selfColumnName != null ? selfColumnName : columnName ) );
			sb.append( " - " ).append( columnType ).append( " - " );
			sb.append( javaType ).append( " - Remarks:" ).append( remarks );
			sb.append( ( isPrimaryKey ? " - Is Primary Key" : "" ) );
			return sb.toString();
		}
	}

	class TableInfo {

		private String tableName;

		private boolean hasPrimaryKey;

		private int columnCount;

		private Map<String,ColumnInfo> columnMap;

		TableInfo(){}

		TableInfo( String tableName ) {
			this.tableName = tableName;
		}

		public String getTableName() {
			return tableName;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
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

		public Map<String, ColumnInfo> getColumnMap() {
			return columnMap;
		}

		public void setColumnMap(Map<String, ColumnInfo> columnMap) {
			this.columnMap = columnMap;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder( "Table:" + tableName );
			sb.append( " - Select Column Count:" ).append( columnCount );
			sb.append( hasPrimaryKey ? " - Has Primary Key" : "" ).append( System.lineSeparator() );
			for( Map.Entry<String, ColumnInfo> entry : columnMap.entrySet() ) {
				sb.append( entry.getKey() ).append( " > " );
				sb.append( entry.getValue() ).append( System.lineSeparator() );
			}
			return sb.toString();
		}
	}
	
	/* 目前套件路徑 */
	/* private String packagePath;*/
	
	/** 作業代碼 */
	private String taskId;
	/** 作業名稱 */
	private String taskName;
	/** 程式說明區塊-描述 */
	private String taskDescription;
	/** 程式說明區塊-資料來源 */
	private String sourceDescription;
	/** 表格名稱 */
	private String tableName;
	/** 查詢欄位 */
	private String columns;
	/** 作者 */
	private String author;
	/** 連線資料庫 */
	private String dbName;
	/** 產製檔案名稱 */
	private String domainObjectName;
	/** 產製Bean名稱 */
	private String domainBeanName;
	/** 產製Dao名稱 */
	private String domainDaoName;
	/** 產製Interface名稱 */
	private String domainInterfaceName;
	/** 目標Bean套件路徑 */
	private String targetBeanPackage;
	/** 目標Dao套件路徑 */
	private String targetDaoPackage;
	/** 目標Interface套件路徑 */
	private String targetInterfacePackage;
	/** 目標Bean產檔路徑 */
	private String targetBeanPath;
	/** 目標Dao產檔路徑 */
	private String targetDaoPath;
	/** 目標Interface產檔路徑 */
	private String targetInterfacePath;
	/** 是否有BigDecimal型態 */
	private Boolean hasBigDecimal = false;
	/** 是否有TIMESTAMP型態 */
	private Boolean hasTimeStamp = false;
	/** 是否有日期型態 */
	private Boolean hasDate = false;
	/** 命名原則 */
	private String namingConventions;
	/** 映射註解 Annotation \@Basic or \@Column */
	private String mappedType;
	/** 是否產檔 Dao & Interface */
	private Boolean needDao;
	/** 現在時間 */
	private String currentTime;

	/** 表格欄位資訊 */
	private TableInfo tableInfo;

	/**
	 *	sample_one 說明：範例一, 示範產生Entity, Dao, Interface<br>
	 *	@author Alan Hsu
	 */
	public void sample_one(CustomCodegen gen) throws Exception {
		/* 作者 */
		gen.setAuthor("Alan");
		/* 產出檔案名稱 */
		gen.setDomainObjectName("SCTest");
		/* DB連線設定,預設在MySQL */
		gen.setDbName("MySQL");
		/* 來源DB表格 */
		gen.setTableName("sctype");
		/* 作業(選單)代號 */
		gen.setTaskId("TEST001");
		/* 作業(選單)名稱 */
		gen.setTaskName("測試查詢");
		/* 程式說明區塊-描述 */
		gen.setTaskDescription("測試查詢功能");
		/* 程式說明區塊-資料來源 */
		gen.setSourceDescription("測試查詢(TestQuery)");
		/* 產出Entity目標路徑 */
		gen.setTargetBeanPath(
				"D:\\workspace\\bean");
		/* 產出Dao目標路徑 */
		gen.setTargetDaoPath(
				"D:\\workspace\\dao");
		/* 產出Interface目標路徑 */
		gen.setTargetInterfacePath("D:\\workspace\\inter");
		/* 產出Entity Annotation 類型： \@Column or \@Basic */
		gen.setMappedType("@Column");
		/* 是否產出Dao, true：產出Entity & Dao & Interface, false：僅產出Entity */
		gen.setNeedDao(true);
		/* 必要設定檢核 */
		if( gen.checkRequiredSetting() ) {
			/* 開始Codegen */
			gen.excute();
		}
	}

	/**
	 *	sample_two 說明：範例二, 只產Entity並示範使用自行設定的欄位數量<br>
	 *	@author Alan Hsu
	 *	@throws Exception
	 */
	public void sample_two(CustomCodegen gen) throws Exception {
		/* 作者 */
		gen.setAuthor("Alan");
		/* 產出檔案名稱 */
		gen.setDomainObjectName("SCTest");
		/* DB連線設定,預設在CIB */
		gen.setDbName("MySQL");
		/* 來源DB表格 */
		gen.setTableName("sctype");
		/* Menu 欄位有 category, menuId, parentMenuId, taskId, orderNo */
		String columns = "category, menuId, parentMenuId, taskId";
		/* 來源表格欄位 - 用來客製化 1.產生的屬性大小寫命名 2.產生的屬性數量(非產生全部Table的欄位) */
		gen.setColumns(columns);
		/* 作業(選單)代號 */
		gen.setTaskId("TEST002");
		/* 作業(選單)名稱 */
		gen.setTaskName("選單查詢測試");
		/* 程式說明區塊-描述 */
		gen.setTaskDescription("測試查詢選單功能");
		/* 程式說明區塊-資料來源 */
		gen.setSourceDescription("選單(Menu)");
		/* 產出Entity目標路徑 */
		gen.setTargetBeanPath(
				"D:\\workspace\\bean");
		/* 產出Dao目標路徑 */
		/*
		gen.setTargetDaoPath(
				"D:\\workspace\\dao");
		*/
		/* 產出Interface目標路徑 */
		/*
		 *	gen.setTargetInterfacePath("D:\\workspace\\inter");
		 */
		
		/* 產出Entity Annotation 類型： \@Column or \@Basic */
		gen.setMappedType("@Basic");
		/* 是否產出Dao, true：產出Entity & Dao & Interface, false：僅產出Entity */
		gen.setNeedDao(false);
		/* 必要設定檢核 */
		if( gen.checkRequiredSetting() ) {
			/* 開始Codegen */
			gen.excute();
		}
	}

	public static void main(String[] args) {
		try {
			CustomCodegen gen = new CustomCodegen();
			gen.sample_one(gen);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 *	excute 說明：產生Bean檔案<br>
	 *	@author Alan Hsu
	 */
	public void excute() throws Exception {
		Date now = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String nowStr = sdf.format(now);
		setCurrentTime(nowStr);
		if (!getTableInfo()) {
			System.err.println(getTableName() + " Table Doesn't Exists or bad SQL.");
			return;
		}
		System.out.println();
		generateBean();
		if (getNeedDao()) {
			generateInterface();
			generateDao();
		}
		System.out.println("### Codegen Process Finished ###");
	}

	/**
	 *	checkRequiredSetting 說明：檢核設定<br>
	 *	@author Alan Hsu
	 *	@return boolean valid
	 */
	public boolean checkRequiredSetting() {
		StringBuilder sb = new StringBuilder("");
		if ("".equals( toCleanString( getDomainObjectName() ) ) ) {
			sb.append("[檔案名稱(DomainObjectName)]").append( System.lineSeparator() );
		}
		if( "".equals( toCleanString( getTableName() ) ) ) {
			sb.append("[表格名稱(TableName)]").append( System.lineSeparator() );
		}
		if( "".equals( toCleanString( getTargetBeanPath() ) ) ) {
			sb.append("[目標Bean路徑(TargetBeanPath)]").append( System.lineSeparator());
		}
		if( getNeedDao() ) {
			if( "".equals( toCleanString( getTargetDaoPath() ) ) ) {
				sb.append("[目標Dao路徑(TargetDaoPath)]").append( System.lineSeparator() );
			}
			if( "".equals( toCleanString( getTargetInterfacePath()) ) ) {
				sb.append("[目標Interface路徑(TargetInterfacePath)]").append( System.lineSeparator() );
			}
		}
		if( !"".equals( sb.toString() ) ) {
			sb.append("必須設定!!!");
		}
		if( !"".equals( sb.toString() ) ) {
			System.err.println( sb );
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
	 *	generateBean 說明：產生Bean檔案<br>
	 *	@author Alan Hsu
	 */
	public void generateBean() {
		try {
			if( tableInfo == null ) {
				System.err.println("無表格資訊");
				return;
			}
			String beanName = getDomainObjectName() + "Bean";
			setDomainBeanName(beanName);
			String newPathStr = getNewFilePath("bean");
			/* System.out.println("newPath =" + newPathStr ); */
			List<String> lines = new ArrayList<String>();
			lines.add(getPackageStr("bean"));
			lines.add(getBeanImportStr());
			lines.add(getBasicBeanImportStr());
			lines.add(getClassInfoBlockStr("bean"));
			lines.add(getBeanClassStartStr(beanName));
			String fieldsSpace = "\t";
			String fieldsSpaceTwo = "\t\t";

			Map<String,ColumnInfo> column = tableInfo.getColumnMap();
			
			StringBuilder fields = new StringBuilder( fieldsSpace + "" + System.lineSeparator() );
			for( Map.Entry<String, ColumnInfo> entry : column.entrySet() ) {
				
				String columnName = entry.getKey();
				ColumnInfo columnData = entry.getValue();
				String cColumnName = "";
				
				if( getColumns() != null && !"".equals( getColumns() ) ) {
					cColumnName = columnData.getSelfColumnName();
				} else {
					cColumnName = convertCase( columnName, "camel" );
				}
				
				//String columnClassName = entry.getValue();
				fields.append( getFieldDescription( columnName, columnData.getRemarks() ) );
				
				if( columnData.isPrimaryKey() ) {
					fields.append( fieldsSpace ).append( "@Id" ).append( System.lineSeparator() );
					if( "@Column".equals( getMappedType() ) ) {
						fields.append( fieldsSpace ).append( "@GeneratedValue(strategy=GenerationType.IDENTITY)" ).append( System.lineSeparator() );
						fields.append( fieldsSpace ).append( "@Column(name=\"" ).append( columnName + "\")" ).append( System.lineSeparator());
					}
					fields.append( fieldsSpace ).append( "private " ).append( columnData.getJavaType() ).append( " " + cColumnName ).append( ';' ).append( System.lineSeparator() );
					fields.append( fieldsSpace ).append( System.lineSeparator() );
				} else {
					if( "@Column".equals( getMappedType() ) ) {
						fields.append( fieldsSpace ).append( "@Column(name=\"" ).append( columnName ).append( "\")" ).append( System.lineSeparator() );
					} else {
						fields.append( fieldsSpace ).append( "@Basic" ).append( System.lineSeparator() );
					}
					fields.append( fieldsSpace ).append( "private " ).append( columnData.getJavaType() ).append( " " + cColumnName ).append( ';' ).append( System.lineSeparator() );
					fields.append( fieldsSpace ).append(  System.lineSeparator() );
				}
			}
			lines.add( fields.toString() );
			fields = null;

			StringBuilder methods = new StringBuilder(fieldsSpace + "" + System.lineSeparator());
			for( Map.Entry<String, ColumnInfo> entry : column.entrySet() ) {
				String columnName = entry.getKey();
				ColumnInfo columnData =entry.getValue();
				
				String cColumnName = "";
				if( getColumns() != null && !"".equals( getColumns() ) ) {
					cColumnName = columnData.getSelfColumnName();
				} else {
					cColumnName = convertCase( columnName, "camel" );
				}
				String pColumnName = "";
				if( getColumns() != null && !"".equals( getColumns() ) ) {
					pColumnName = convertCase(columnName, "capitalizeOnlyFirst");
				} else {
					pColumnName = convertCase(columnName, "pascal");
				}

				//String columnClassName = entry.getValue();
				if( columnData.isPrimaryKey() ) {
					methods.append( getMethodGetterDescription( columnName ) );
					methods.append( fieldsSpace ).append( "public " ).append( columnData.getJavaType() ).append( " get" ).append( pColumnName ).append( "() {" ).append( System.lineSeparator() );
					methods.append( fieldsSpace ).append( fieldsSpace ).append( "return " ).append( cColumnName ).append( ';' ).append( System.lineSeparator() );
					methods.append( fieldsSpace ).append( '}' ).append( System.lineSeparator() );
					methods.append( fieldsSpace ).append( System.lineSeparator() );
					methods.append( getMethodSetterDescription( columnName ) );
					methods.append( fieldsSpace ).append( "public void set" ).append( pColumnName ).append( "( ");
					methods.append( columnData.getJavaType() ).append( " " ).append( cColumnName ).append( " ) {" ).append( System.lineSeparator() );
					methods.append( fieldsSpaceTwo ).append( "this." ).append( cColumnName ).append( " = " ).append( cColumnName ).append( ';' ).append( System.lineSeparator() );
					methods.append( fieldsSpace ).append( '}' ).append( System.lineSeparator() );
					methods.append( fieldsSpace ).append( System.lineSeparator() );
				} else {
					methods.append( getMethodGetterDescription( columnName ) );
					methods.append( fieldsSpace ).append( "public " ).append( columnData.getJavaType() ).append( " get" ).append( pColumnName ).append( "() {" ).append( System.lineSeparator() );
					methods.append( fieldsSpaceTwo ).append( "return " ).append( cColumnName ).append(';' ).append( System.lineSeparator() );
					methods.append( fieldsSpace ).append( '}' ).append( System.lineSeparator() );
					methods.append( fieldsSpace ).append( System.lineSeparator() );
					methods.append( getMethodSetterDescription( columnName ) );
					methods.append( fieldsSpace ).append( "public void set" ).append( pColumnName ).append( "( " );
					methods.append( columnData.getJavaType() ).append( " " ).append( cColumnName ).append( " ) {" ).append( System.lineSeparator() );
					methods.append( fieldsSpaceTwo ).append( "this." ).append( cColumnName ).append( " = " ).append( cColumnName ).append( ';' ).append( System.lineSeparator() );
					methods.append( fieldsSpace ).append( '}' ).append( System.lineSeparator() );
					methods.append( fieldsSpace ).append( System.lineSeparator() );
				}
			}
			lines.add( methods.toString() );
			methods = null;

			lines.add( getClassEndStr() );

			Path newPath = Paths.get( newPathStr );
			if( !Files.exists( newPath ) ) {
				Files.createDirectories( newPath );
				System.out.println("Directory created.");
			} else {
				System.out.println("Directory already exists.");
			}
			String newFilePath = newPathStr + File.separator + beanName + ".java";
			Path file = Paths.get( newFilePath );
			System.out.println("New Entity Path =" + newFilePath);
			Files.write( file, lines, StandardCharsets.UTF_8 );
			System.out.println("[ " + beanName + ".java ] generated successful." + System.lineSeparator() );
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}

	/**
	 *	generateInterface 說明：產生Interface檔案<br>
	 *	@author Alan Hsu
	 */
	public void generateInterface() {
		try {
			if( tableInfo == null ) {
				System.err.println("無表格資訊");
				return;
			}
			String interfaceName = getDomainObjectName() + "Interface";
			setDomainInterfaceName( interfaceName );
			String newPathStr = getNewFilePath("Inter");
			/* System.out.println("newPath =" + newPathStr ); */
			List<String> lines = new ArrayList<String>();
			lines.add( getPackageStr("Inter") );
			lines.add( getBasicInterfaceImportStr() );
			lines.add( getClassInfoBlockStr("Inter") );
			lines.add( getInterfaceClassStartStr( interfaceName ) );
			lines.add( getClassEndStr() );
			Path newPath = Paths.get( newPathStr );
			if( !Files.exists( newPath ) ) {
				Files.createDirectories(newPath);
				System.out.println("Directory created.");
			} else {
				System.out.println("Directory already exists.");
			}
			String newFilePath = newPathStr + File.separator + interfaceName + ".java";
			Path file = Paths.get( newFilePath );
			System.out.println( "New Interface Path =" + newFilePath );
			Files.write( file, lines, StandardCharsets.UTF_8 );
			System.out.println("[ " + interfaceName + ".java ] generated successful." + System.lineSeparator() );
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}

	/**
	 *	generateDao 說明：產生Dao檔案<br>
	 *	@author Alan Hsu
	 */
	public void generateDao() {
		try {
			if( tableInfo == null ) {
				System.err.println("無表格資訊");
				return;
			}
			String daoName = getDomainObjectName() + "Dao";
			setDomainDaoName( daoName );
			String newPathStr = getNewFilePath("dao");
			/*  System.out.println("newPath =" + newPathStr ); */
			List<String> lines = new ArrayList<String>();
			lines.add( getPackageStr("dao") );
			lines.add( getBasicDaoImportStr() );
			lines.add( getClassInfoBlockStr("dao") );
			lines.add( getDaoClassStartStr(daoName) );
			lines.add( getClassEndStr() );
			Path newPath = Paths.get(newPathStr);
			if( !Files.exists( newPath ) ) {
				Files.createDirectories( newPath );
				System.out.println("Directory created.");
			} else {
				System.out.println("Directory already exists.");
			}
			String newFilePath = newPathStr + File.separator + daoName + ".java";
			Path file = Paths.get( newFilePath );
			System.out.println("New Dao Path =" + newFilePath);
			Files.write( file, lines, StandardCharsets.UTF_8 );
			System.out.println("[ " + daoName + ".java ] generated successful." + System.lineSeparator() );
		} catch( IOException e ) {
			e.printStackTrace();
		}

	}

	/**
	 *	getBasicImportStr 說明：取得套件路徑字串<br>
	 *	@return String packageStr
	 *	@author Alan Hsu
	 */
	public String getPackageStr( String type ) {
		type = type.toLowerCase();
		String str = "";
		if( "bean".equals( type ) ) {
			str = getTargetBeanPackage();
		} else if( "dao".equals( type ) ) {
			str = getTargetDaoPackage();
		} else if( "inter".equals( type ) ) {
			str = getTargetInterfacePackage();
		}
		return "package " + str + ';' + System.lineSeparator();
	}

	/**
	 *	getBasicBeanImportStr 說明：取得Bean必要引用字串<br>
	 *	@return String basicBeanImportStr
	 *	@author Alan Hsu
	 */
	public String getBasicBeanImportStr() {
		StringBuilder sb = new StringBuilder("");
		if( "@Column".equals( getMappedType() ) ) {
			sb.append("import javax.persistence.Column;" ).append( System.lineSeparator() );
		} else {
			sb.append("import javax.persistence.Basic;" ).append( System.lineSeparator() );
		}
		sb.append("import javax.persistence.Entity;" ).append( System.lineSeparator() );
		if( tableInfo.getHasPrimaryKey() ) {
			if( "@Column".equals( getMappedType() ) ) {
				sb.append( "import javax.persistence.GeneratedValue;" ).append( System.lineSeparator() );
				sb.append( "import javax.persistence.GenerationType;" ).append( System.lineSeparator() );
			}
			sb.append( "import javax.persistence.Id;" ).append( System.lineSeparator() );
		}
		sb.append( "import javax.persistence.Table;" ).append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 *	getBasicInterfaceImportStr 說明：取得Interface必要引用字串<br>
	 *	@return String basicInterfaceImportStr
	 *	@author Alan Hsu
	 */
	public String getBasicInterfaceImportStr() {
		StringBuilder sb = new StringBuilder( System.lineSeparator());
		/*
		if( "highSchool".equals( getDbName() ) ) {
			if( !getTargetInterfacePackage().contains(".highSchool.dao") ) {
				sb.append( "import com.tw.persistence.highSchool.dao.ISCHDao;" ).append( System.lineSeparator() );
			}
		} else {
			if( !getTargetInterfacePackage().contains(".college.dao") ) {
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
		/*
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
		sb.append( "import " ).append( getTargetInterfacePackage() ).append( '.' ).append( getDomainInterfaceName() ).append( ';' ).append( System.lineSeparator() );
		sb.append( "import " ).append( getTargetBeanPackage() ).append( '.' ).append( getDomainBeanName() ).append( ';' ).append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 *	getClassInfoBlockStr 說明：取得程式說明區塊字串<br>
	 *	@param type the type of file name
	 *	@return String classStartStr
	 *	@author Alan Hsu
	 */
	public String getClassInfoBlockStr( String type ) {
		StringBuilder sb = new StringBuilder("");
		sb.append( "/**" ).append( System.lineSeparator() );
		sb.append( " * 作 業 代 碼 ：" ).append( toCleanString( getTaskId() ) ).append( "<br>" ).append( System.lineSeparator() );
		sb.append( " * 作 業 名 稱 ：" ).append( toCleanString( getTaskName() ) ).append( "<br>" ).append( System.lineSeparator() );
		sb.append( " * 程 式 代 號 ：" ).append( getFileNameByType( type ) ).append( ".java<br>" ).append( System.lineSeparator() );
		sb.append( " * 描 述 ：" ).append( toCleanString( getTaskDescription() ) ).append( "<br>" ).append( System.lineSeparator() );
		sb.append( " * 公 司 ：Tenpastten Studio<br>" ).append( System.lineSeparator() );
		sb.append( " * 【 資 料 來 源】 ：" ).append( getSourceDescription() ).append( "<br>" ).append( System.lineSeparator() );
		sb.append( " * 【 異 動 紀 錄】 ：<br>" ).append( System.lineSeparator() );
		sb.append( " *" ).append( System.lineSeparator() );
		sb.append( " * @author : " ).append( author ).append( "<br>" ).append( System.lineSeparator() );
		sb.append( " * @version : 1.0.0 " ).append( getCurrentTime() ).append( "<br>" ).append( System.lineSeparator() );
		sb.append( " */" );
		return sb.toString();
	}

	/**
	 *	getBeanClassStartStr 說明：取得Bean新檔案class名稱<br>
	 *	@param entityName the entity name
	 *	@return String classStartStr
	 *	@author Alan Hsu
	 */
	public String getBeanClassStartStr( String entityName ) {
		StringBuilder sb = new StringBuilder("");
		sb.append( "@Entity" ).append( System.lineSeparator() );
		sb.append( "@Table(name=\"" ).append( getTableName() ).append( "\")" ).append( System.lineSeparator() );
		sb.append( "public class " ).append( entityName ).append( " {" ).append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 *	getInterfaceClassStartStr 說明：取得Interface新檔案class名稱<br>
	 *	@param entityName the entity name
	 *	@return String classStartStr
	 *	@author Alan Hsu
	 */
	public String getInterfaceClassStartStr( String entityName ) {
		StringBuilder sb = new StringBuilder("");
		sb.append( "public interface " ).append( entityName ).append( " " );
		/*
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
	 *	getDaoClassStartStr 說明：取得Dao新檔案class名稱<br>
	 *	@param entityName the entity name
	 *	@return String classStartStr
	 *	@author Alan Hsu
	 */
	public String getDaoClassStartStr( String entityName ) {
		StringBuilder sb = new StringBuilder("");
		sb.append( "public class " ).append( entityName ).append( " ");
		/*
		if( "highSchool".equals( getDbName() ) ) {
			sb.append("extends SCHDao<" + getDomainBeanName() + "> ");
		} else {
			sb.append("extends COLDao<" + getDomainBeanName() + "> ");
		}
		*/
		sb.append( "implements " ).append( getDomainInterfaceName() ).append( " " );
		sb.append( '{' ).append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 *	getClassEndStr 說明：取得產檔class的結尾大括號<br>
	 *	@return String classEndStr
	 *	@author Alan Hsu
	 */
	public static String getClassEndStr() {
		StringBuilder sb = new StringBuilder("");
		sb.append( '}' ).append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 *	getBeanImportStr 說明：取得Bean需要引用的其他型別<br>
	 *	@return String importStr
	 *	@author Alan Hsu
	 */
	public String getBeanImportStr() {
		StringBuilder sb = new StringBuilder("");
		if( getHasBigDecimal() ) {
			sb.append( "import java.math.BigDecimal;" ).append( System.lineSeparator() );
		}
		if( getHasTimeStamp() ) {
			sb.append( "import java.sql.Timestamp;" ).append( System.lineSeparator() );
		}
		if( getHasDate() ) {
			sb.append( "import java.util.Date;" ).append( System.lineSeparator() );
		}
		return sb.toString();
	}

	/**
	 *	setColumnTypeImport 說明：設定是否需要引用其他型別之旗標<br>
	 *	@param columnTypeName java type of column
	 *	@author Alan Hsu
	 */
	public void checkColumnTypeImport( String columnTypeName ) {
		switch( columnTypeName.toUpperCase() ) {
		case "BIGDECIMAL":
			setHasBigDecimal( true );
			break;
		case "DATE":
			setHasDate( true );
			break;
		case "TIMESTAMP":
			setHasTimeStamp( true );
			break;
		}
	}

	/**
	 *	getFieldDescription 說明：取得成員變數說明<br>
	 *	@return String importStr
	 *	@author Alan Hsu
	 */
	public String getFieldDescription( String columnsName, String remarks ) {
		String fieldsSpace = "\t";
		StringBuilder sb = new StringBuilder();
		sb.append( fieldsSpace ).append( "/**" ).append( System.lineSeparator() );
		sb.append( fieldsSpace ).append( " * This field was generated by CustomCodegen Generator. " );
		sb.append( "This field corresponds to the database column " ).append( getTableName().toUpperCase() );
		sb.append( '.' ).append( columnsName ).append( System.lineSeparator() );
		sb.append( fieldsSpace ).append( " * Remarks: " ).append( ( remarks == null ? "NONE" : remarks ) ).append( System.lineSeparator() );
		sb.append( fieldsSpace ).append( " * cg.generated " ).append( getCurrentTime() ).append( System.lineSeparator() );
		sb.append( fieldsSpace ).append( " */" ).append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 *	getMethodSetterDescription 說明：取得設值方法說明<br>
	 *	@return String str
	 *	@author Alan Hsu
	 */
	public String getMethodSetterDescription( String columnsName ) {
		String fieldsSpace = "\t";
		String correspondsCol = getTableName() + '.' + columnsName;
		StringBuilder sb = new StringBuilder();
		sb.append( fieldsSpace ).append( "/**" ).append( System.lineSeparator() );
		sb.append( fieldsSpace ).append( " * This method was generated by CustomCodegen Generator. " );
		sb.append( "This method sets the value of the database column " ).append( correspondsCol ).append( System.lineSeparator() );
		sb.append( fieldsSpace ).append( " * @param " ).append( toCamelCase( columnsName ) );
		sb.append( " the value for " ).append( correspondsCol ).append( System.lineSeparator() );
		sb.append( fieldsSpace ).append( " * cg.generated " ).append( getCurrentTime() ).append( System.lineSeparator() );
		sb.append( fieldsSpace ).append( " */" ).append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 *	getMethodGetterDescription 說明：取得取值方法說明<br>
	 *	@return String str
	 *	@author Alan Hsu
	 */
	public String getMethodGetterDescription( String columnsName ) {
		String fieldsSpace = "\t";
		String correspondsCol = getTableName() + '.' + columnsName;
		StringBuilder sb = new StringBuilder("");
		sb.append( fieldsSpace ).append( "/**" ).append( System.lineSeparator() );
		sb.append( fieldsSpace ).append( " * This method was generated by CustomCodegen Generator. " );
		sb.append( "This method returns the value of the database column " ).append( correspondsCol ).append( System.lineSeparator() );
		sb.append( fieldsSpace ).append( " * @return " ).append( toCamelCase( columnsName ) );
		sb.append( " the value of " ).append( correspondsCol ).append( System.lineSeparator() );
		sb.append( fieldsSpace ).append( " * cg.generated " ).append( getCurrentTime() ).append( System.lineSeparator() );
		sb.append( fieldsSpace ).append( " */" ).append( System.lineSeparator() );
		return sb.toString();
	}

	/**
	 *	getNewFilePath 說明：取得新檔案路徑<br>
	 *	@param type type of file
	 *	@return String newFilePath
	 *	@author Alan Hsu
	 */
	public String getNewFilePath( String type ) {
		type = type.toLowerCase();
		String sep = File.separator;
		String path = "";
		String packagePath = "";
		if( "".equals( type ) ) {
			/* Working Directory*/
			String directory = Paths.get(".").toAbsolutePath().normalize().toString();
			/* Package*/
			packagePath = CustomCodegen.class.getPackage().getName();
			setTargetBeanPackage( packagePath );
			String packageStr = packagePath.replaceAll( "\\.", Matcher.quoteReplacement( sep ) );
			path = directory + sep + "src" + sep + packageStr;
		} else if( "bean".equals( type ) ) {
			String directory = getTargetBeanPath();
			directory = directory.replaceAll( "\\.", Matcher.quoteReplacement( sep ) );
			path = directory;
			/* Bean Package*/
			int srcIdx = directory.indexOf("src\\main\\java");
			String srcStr = directory.substring( srcIdx + 14 );
			packagePath = srcStr.replaceAll( "\\\\", "." );
			setTargetBeanPackage( packagePath );
		} else if( "inter".equals( type ) ) {
			String directory = getTargetInterfacePath();
			directory = directory.replaceAll( "\\.", Matcher.quoteReplacement( sep ) );
			path = directory;
			/* Interface Package*/
			int srcIdx = directory.indexOf("src\\main\\java");
			String srcStr = directory.substring( srcIdx + 14 );
			packagePath = srcStr.replaceAll("\\\\", ".");
			setTargetInterfacePackage(packagePath);
		} else if( "dao".equals( type ) ) {
			String directory = getTargetDaoPath();
			directory = directory.replaceAll("\\.", Matcher.quoteReplacement( sep ) );
			path = directory;
			/* Dao Package*/
			int srcIdx = directory.indexOf("src\\main\\java");
			String srcStr = directory.substring( srcIdx + 14 );
			packagePath = srcStr.replaceAll("\\\\", ".");
			setTargetDaoPackage( packagePath );
		}
		System.out.println("path =" + path);
		System.out.println("packagePath =" + packagePath );
		return path;
	}

	/**
	 *	getFileNameByType 說明：取得對應的檔案名稱<br>
	 *	@param type
	 *	@return String fileName
	 *	@author Alan Hsu
	 */
	public String getFileNameByType( String type ) {
		type = type.toLowerCase();
		String fileName = "";
		if( "bean".equals( type ) ) {
			fileName = getDomainBeanName();
		} else if( "dao".equals( type ) ) {
			fileName = getDomainDaoName();
		} else if( "inter".equals( type ) ) {
			fileName = getDomainInterfaceName();
		}
		return fileName;
	}

	/**
	 * getTableInfo 說明：取得表格資訊<br>
	 * @return boolean tableInfoExists 
	 * @author Alan Hsu
	 */
	public boolean getTableInfo() throws Exception {
		TableInfo tableInfo = new TableInfo( getTableName() );
		Connection conn = null;
		try {
			conn = getConnection();
			Statement stmt = conn.createStatement();
			boolean useSelfColumn = false;
			String columns = getColumns();
			List<String> selfList = null;
			if( columns == null || "".equals( columns ) ) {
				columns = "*";
			} else {
				useSelfColumn = true;
				String[] selfAry = columns.trim().split(",");
				selfList = new ArrayList<String>();
				for( String s : selfAry ) {
					selfList.add( s.trim() );
				}
			}
			String sql = "SELECT " + columns + " FROM " + getTableName() + " WHERE 1 = 2";
			System.out.println( sql );
			ResultSet rs = stmt.executeQuery( sql );
			ResultSetMetaData rsmd = rs.getMetaData();
			DatabaseMetaData dbmd = conn.getMetaData();
			int columnCount = rsmd.getColumnCount();
			tableInfo.setColumnCount( columnCount );
			Map<String, ColumnInfo> columnMap = new LinkedHashMap<String, ColumnInfo>();
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
				 * System.out.println( columnName + ", columnType:" + columnType + ",
				 * javaType:" + columnClassName );
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
			/** To Get Column PK */
			rs = dbmd.getPrimaryKeys( null, null, tableName );
			while( rs.next() ) {
				String pKey = rs.getString("COLUMN_NAME");
				columnMap.get( pKey ).setPrimaryKey( true );
				tableInfo.setHasPrimaryKey( true );
			}
			/** To Get Column Remarks */
			rs = dbmd.getColumns( null, null, getTableName(), null );
			while( rs.next() ) {
				String remarks = rs.getString("REMARKS");
				String columnName = rs.getString("COLUMN_NAME");
				columnMap.get( columnName ).setRemarks( remarks );
			}
			rs = null;
			tableInfo.setColumnMap( columnMap );
			setTableInfo( tableInfo );
		} catch( SQLSyntaxErrorException e ) {
			e.printStackTrace();
			return false;
		} catch( Exception e ) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				conn.close();
				System.out.println("### Connection closed ###");
			} catch( SQLException e ) {
				e.printStackTrace();
			}
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
		String serverUrl = "jdbc:oracle:thin:@10.XXX.XXX.XX:15XX:XXXXDB";
		return DriverManager.getConnection( serverUrl, "xxx_user", "xxx_pwd" );
	}

	/**
	 *	getMySQLConnection 說明：取得MySQL DB連線<br>
	 *	@return Connection
	 *	@author Alan Hsu
	 */
	public Connection getMySQLConnection() throws Exception {
		Class.forName("com.mysql.cj.jdbc.Driver");
		String serverUrl = "jdbc:mysql://localhost:3306/XXXX?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
		return DriverManager.getConnection( serverUrl, "USERXXXX", "PWDXXXX" );
	}

	/**
	 *	convertCase 說明：依據命名規範設定,調整字串<br>
	 *
	 *	@param txt the word to be case
	 *	@return String result
	 *	@author Alan Hsu
	 */
	public String convertCase( String txt ) {
		String naming = toCleanString( getNamingConventions() );
		String result = "";
		switch( naming ) {
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
	 *	convertCase 說明：依據命名規範設定,調整字串<br>
	 *	@param txt the word to be case
	 *	@return String result
	 *	@author Alan Hsu
	 */
	public String convertCase( String txt, String naming ) {
		String result = "";
		switch( naming ) {
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
	 *	toCamelCase 說明：欄位資訊字串轉駝峰命名(E.g. camelCase)<br>
	 *	@param text the word to be case
	 *	@return String camel
	 *	@author Alan Hsu
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
	 *	toPascalCase 說明：欄位資訊字串轉帕斯卡命名(E.g. PascalCase)<br>
	 *	@param text the word to be case
	 *	@return String pascal
	 *	@author Alan Hsu
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
	 *	toSnakeCase 說明：欄位資訊字串轉蛇形命名(E.g. Snake_Case)<br>
	 *	@param text the word to be case
	 *	@return String text
	 *	@author Alan Hsu
	 */
	public String toSnakeCase( String text ) {
		return text.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
	}

	/**
	 *	capitalizeFirst 說明：字首轉大寫,其餘轉小寫<br>
	 *	@param word the word to be case
	 *	@return String word
	 *	@author Alan Hsu
	 */
	private String capitalizeFirst( String word ) {
		return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
	}

	/**
	 *	capitalizeOnlyFirst 說明：只將字首轉大寫<br>
	 *	@param word the word to be case
	 *	@return String word
	 *	@author Alan Hsu
	 */
	private String capitalizeOnlyFirst( String word ) {
		return word.substring(0, 1).toUpperCase() + word.substring(1);
	}

	/**
	 *	toCleanString 說明：將參數字串null轉為空字串並且去空白<br>
	 *	@param  paramObj the obj to be case to string
	 *	@return String strVal
	 *	@author Alan Hsu
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

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Boolean getHasBigDecimal() {
		return hasBigDecimal;
	}

	public void setHasBigDecimal(Boolean hasBigDecimal) {
		this.hasBigDecimal = hasBigDecimal;
	}

	public Boolean getHasDate() {
		return hasDate;
	}

	public void setHasDate(Boolean hasDate) {
		this.hasDate = hasDate;
	}

	public Boolean getHasTimeStamp() {
		return hasTimeStamp;
	}

	public void setHasTimeStamp(Boolean hasTimeStamp) {
		this.hasTimeStamp = hasTimeStamp;
	}

	public String getColumns() {
		return columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public void setTableInfo( TableInfo tableInfo) {
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

	public String getDomainInterfaceName() {
		return domainInterfaceName;
	}

	public void setDomainInterfaceName(String domainInterfaceName) {
		this.domainInterfaceName = domainInterfaceName;
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

	public String getTargetInterfacePackage() {
		return targetInterfacePackage;
	}

	public void setTargetInterfacePackage(String targetInterfacePackage) {
		this.targetInterfacePackage = targetInterfacePackage;
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

	public String getTargetInterfacePath() {
		return targetInterfacePath;
	}

	public void setTargetInterfacePath(String targetInterfacePath) {
		this.targetInterfacePath = targetInterfacePath;
	}

	public Boolean getNeedDao() {
		return needDao;
	}

	public void setNeedDao(Boolean needDao) {
		this.needDao = needDao;
	}
}
