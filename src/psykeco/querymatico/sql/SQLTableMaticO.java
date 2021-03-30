package psykeco.querymatico.sql;

import static psykeco.querymatico.sql.utility.SQLClassParser.getTrueName;
import static psykeco.querymatico.sql.utility.SQLClassParser.parseClass;
import static psykeco.querymatico.sql.utility.SQLClassParser.parseType;
import static psykeco.querymatico.sql.utility.SQLClassParser.validateBase;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import psykeco.querymatico.TableMaticO;
import psykeco.querymatico.sql.runners.InformationSchema;
import psykeco.querymatico.sql.runners.MySqlConnection;
import psykeco.querymatico.sql.utility.SQLClassParser;

/**
 * SQLTableMaticO costruisce istruzioni SQL 
 * per creare, distruggere o chiedere se esiste
 *  una tabella a partire da una classe java.
 * 
 * Per farlo, usa le reflection e legge tutti i campi, ogni 
 * campo diventa una colonna e il nome delle classe viene 
 * usato come nome per la tabella <br><br>
 * 
 * &Egrave; Possibile specificare alcuni dettagli come la chiave primaria oppure modificare il nome dei campi con suffissi e prefissi
 * 
 * @author psykedady
 **/
public class SQLTableMaticO implements TableMaticO{
	
	
	/** nome tabella (obbligatorio) */
	private String table;
	/** nome db (obbligatorio) */
	private String db;
	/** suffisso, si agginge dopo i nomi */
	private String suffix="";
	/** prefisso, si agginge dopo i nomi */
	private String prefix="";
	/** mappa <nome,tipo> che viene usata per creare le colonne della tabella */
	private Map<String,String> kv =new HashMap<>();
	/** lista delle chiavi primarie */
	private List<String> primary = new LinkedList<>();
	/** la classe rappresentativa della tabella */
	private Class<?> type;
	
	
	/**
	 * data una stringa, attacca prefisso e suffisso per generare il nuovo nome
	 * @param what stringa in input
	 * @return prefisso+what+suffisso
	 */
	private String attachPreSuf(String what) {
		return validateBase(prefix+what+suffix);
	}
	
	/** costruttore vuoto */
	public SQLTableMaticO() {}
	
	/**
	 * costruttore che , prende in input il db e la classe da trasformare in tabella
	 * 
	 * @param db nome db
	 * @param c la classe che diventer&agrave tabella 
	 */
	@SuppressWarnings("rawtypes")
	public SQLTableMaticO(String db, Class c) {
		this.db=db;
		table(c);
	}
	
	@Override
	public SQLTableMaticO DB(String db) {
		this.db=db;
		return this;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public SQLTableMaticO table(Class c) {
		type=c;
		table=getTrueName(c);
		kv=parseClass(c);
		return this;
	}
	
	
	public SQLTableMaticO suffix(String suffix) { 
		if(suffix!=null)
			this.suffix=suffix; 
		return this; 
	}
	
	
	public SQLTableMaticO prefix(String prefix) { 
		if(prefix!=null)
			this.prefix=prefix; 
		return this; 
	}

	
	public SQLTableMaticO primary(String key) { 
		if(! kv.containsKey(key) ) throw new IllegalArgumentException("La chiave primaria deve riferirsi ad una colonna reale");
		primary.add(key);
		return this;
	}

	public String validate() {
		
		if (table==null || table.equals("")) return "nome tabella necessario";
		if (db   ==null || db   .equals("")) return "nome db necessario"     ;
		if ( kv.size() < 1 ) return "Questa classe non ha parametri, non puo' essere trasformata";
		
		String tmp=validateBase(db);
		if (tmp==null) return "nome db "+db+" non valido";
		db=tmp;		
		
		tmp=validateBase(table);
		if (tmp==null) return "nome tabella "+table+" non valido";
		
		String tmp2=validateBase(prefix+table);
		if (tmp2==null) return "prefisso "+ prefix +" scelto non valido";
		
		tmp2=validateBase(table+suffix);
		if (tmp2==null) return "suffisso "+ suffix +" scelto non valido";
		
		table=tmp;
		
		for (Entry<String,String> kv : kv.entrySet()) {
			if (kv.getKey()  == null || kv.getKey().equals("") ) return "Una colonna \u00e8 stata trovata vuota";
			if ( validateBase(kv.getKey())==null ) return "La colonna "+kv.getKey()+" non \u00e8 valida";
		}
		
		return "";
	}
	
	
	public String create() {
		
		StringBuilder sb=new StringBuilder(kv.size()*20);
		String thisdb=this.db;
		this.db=(this.db==null)? MySqlConnection.db():this.db;
		String db=validateBase(this.db), table =attachPreSuf(this.table);
		
		String validation=validate();
		
		if(!validation.equals("")) throw new IllegalArgumentException(validation);
		
		sb.append("CREATE TABLE `"+db+"`.`"+table+"` (");
		
		for (Entry<String,String> kv :this.kv.entrySet() ) {
			boolean isPrimary=primary.contains(kv.getKey());
			String parsedType=parseType(kv.getValue(),isPrimary);
			String key=validateBase(kv.getKey());
			sb.append(key+' '+parsedType+",");
		}
		
		if(! primary.isEmpty()) {
			sb.append("PRIMARY KEY(");

			for (String k : primary) sb.append(validateBase(k)+',');
			sb.setCharAt(sb.length()-1,')');
			sb.append(',');
		}
		
		sb.setCharAt(sb.length()-1, ')');
		
		this.db=thisdb;
		return sb.toString();
	}

	@Override
	public String exists() {
		return InformationSchema.existTableBuild(db, table);
	}

	@Override
	public String drop() {
		String thisdb=this.db;
		this.db=(this.db==null)? MySqlConnection.db():this.db;
		String validation=validate();
		String db=validateBase(this.db), table =attachPreSuf(this.table);

		if(!validation.equals("")) throw new IllegalArgumentException(validation);
		
		String sb="DROP TABLE IF EXISTS `"+db+"`.`"+table+"`";
		
		this.db=thisdb;
		return sb;
	}

	@Override
	public SQLInsertMaticO insertData(Object o) {
		String db=validateBase(this.db), table= attachPreSuf(this.table); 

		SQLInsertMaticO qc=new SQLInsertMaticO().DB(db).table(table);
		Map<String,Object> map=SQLClassParser.parseInstance(type, o);
		
		for (Entry<String,Object> entry : map.entrySet()) {
			if(entry.getValue()==null) continue;
			qc.entry(entry);
		}
		
		return qc;
	}

	@Override
	public SQLSelectMaticO selectData(Object o) { 
		String table= attachPreSuf(this.table);
		
		SQLSelectMaticO qc=new SQLSelectMaticO().DB(db).table(table);
		if ( o!= null ) { 
			Map<String,Object> map=SQLClassParser.parseInstance(type, o);
		
			for (Entry<String,Object> entry : map.entrySet()) {
				if(entry.getValue()==null) continue;
				qc.filter(entry);
			}
		}
		
		return qc;
	}

	@Override
	public SQLDeleteMaticO deleteData(Object o) {
		String table= attachPreSuf(this.table);

		SQLDeleteMaticO qc=new SQLDeleteMaticO().DB(db).table(table);
		
		Map<String,Object> map=SQLClassParser.parseInstance(type, o);
		
		for (Entry<String,Object> entry : map.entrySet()) {
			if(entry.getValue()==null) continue;
			qc.filter(entry);
		}
		
		return qc;
	}

	@Override
	public SQLUpdateMaticO updateData(Object o) {
		String table= attachPreSuf(this.table);

		SQLUpdateMaticO qc=new SQLUpdateMaticO().DB(db).table(table);
		
		Map<String,Object> map=SQLClassParser.parseInstance(type, o);
		
		for (Entry<String,Object> entry : map.entrySet()) {
			if(primary.contains(entry.getKey()) ) {
				if(entry.getValue()==null) 
					throw new IllegalArgumentException("Gli elementi nella chiave primaria non possono essere null");
				
				qc.filter(entry);
			} else {
				if(entry.getValue()==null) continue;
				
				qc.entry(entry);
			}
		}
		
		return qc;
	}

	@Override
	public SQLSelectMaticO countData(Object o) {
		SQLSelectMaticO qc=new SQLSelectMaticO().DB(db).table(table);
		
		if ( o!= null ) { 
			Map<String,Object> map=SQLClassParser.parseInstance(type, o);
		
			for (Entry<String,Object> entry : map.entrySet()) {
				if(entry.getValue()==null) continue;
				qc.filter(entry);
			}
		} 
		qc.count(null);
		
		return qc;
	}

	@Override
	public SQLTableMaticO copy() {
		SQLTableMaticO tf= new SQLTableMaticO().DB(db).prefix(prefix).suffix(suffix);
		if (table!=null && kv!=null) tf.table(type);
		if (primary!=null) for (String key : primary)
			tf.primary(key);
		
		return tf;
	}
	
}