
# CustomCodegen

## Requirements
- Java >= 1.8
- OJDBC8

## Introduction

This is Custom Codegen for some legacy frameworks, to auto generate java bean & dao interface and dao.

## Synopsis

Usage:

```java
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
```


