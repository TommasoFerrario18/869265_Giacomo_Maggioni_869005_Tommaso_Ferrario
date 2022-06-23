Nella creazione del progetto abbiamo utilizato le seguenti librerie: GSON, 
sqlite-jdbc. Le abbiamo importate utilizzando il Maven. 
Tutte le dipendenze che abbiamo utilizzato dovrebbero essere già presenti nel 
progetto, in caso contrario è sufficiente inserire nel tag dependencies del file 
pom.xml le dipendenze qui sotto riportate
Dipendeze:
- GSON: è una libreria Java che può essere utilizzata per convertire gli oggetti 
 Java nella loro rappresentazione JSON. 
<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
  <version>2.9.0</version>
</dependency>
- sqlite-jdbc: è una libreria per l'accesso e la creazione di file di database 
 SQLite in Java.
<dependency>
	<groupId>org.xerial</groupId>
	<artifactId>sqlite-jdbc</artifactId>
	<version>3.36.0.3</version>
</dependency>

