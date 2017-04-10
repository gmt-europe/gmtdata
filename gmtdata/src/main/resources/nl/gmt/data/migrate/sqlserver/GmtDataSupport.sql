IF NOT EXISTS(SELECT * FROM [sys].[tables] WHERE [name] = 'gmtdataschema')
BEGIN
	CREATE TABLE [dbo].[gmtdataschema]
	(
		[key] NVARCHAR(200) NOT NULL PRIMARY KEY,
		[value] NVARCHAR(MAX) NOT NULL
	)
END