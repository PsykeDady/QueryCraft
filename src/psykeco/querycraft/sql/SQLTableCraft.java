package psykeco.querycraft.sql;

import static psykeco.querycraft.utility.SQLClassParser.getTrueName;
import static psykeco.querycraft.utility.SQLClassParser.parseClass;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import psykeco.querycraft.QueryCraft;
import psykeco.querycraft.SelectCraft;
import psykeco.querycraft.TableCraft;
import psykeco.querycraft.utility.SQLClassParser;

/**
 * SQLTableCraft costruisce istruzioni SQL 
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
public class SQLTableCraft implements TableCraft{
	
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
	@SuppressWarnings("rawtypes")
	private Class type;
	
	
	/**
	 * data una stringa, attacca prefisso e suffisso per generare il nuovo nome
	 * @param what stringa in input
	 * @return prefisso+what+suffisso
	 */
	private String attachPreSuf(String what) {
		return prefix+what+suffix;
	}
	
	/** costruttore vuoto */
	public SQLTableCraft() {}
	
	/**
	 * costruttore che , prende in input il db e la classe da trasformare in tabella
	 * 
	 * @param db nome db
	 * @param c la classe che diventer&agrave tabella 
	 */
	@SuppressWarnings("rawtypes")
	public SQLTableCraft(String db, Class c) {
		this.db=db;
		table(c);
	}
	
	@Override
	public SQLTableCraft DB(String db) {
		this.db=db;
		return this;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public SQLTableCraft table(Class c) {
		type=c;
		table=getTrueName(c);
		kv=parseClass(c);
		return this;
	}
	
	
	public SQLTableCraft suffix(String suffix) { 
		if(suffix!=null)
			this.suffix=suffix; 
		return this; 
	}
	
	
	public SQLTableCraft prefix(String prefix) { 
		if(prefix!=null)
			this.prefix=prefix; 
		return this; 
	}

	
	public SQLTableCraft primary(String key) { 
		if(! kv.containsKey(key) ) throw new IllegalArgumentException("La chiave primaria deve riferirsi ad una colonna reale");
		primary.add(key);
		return this;
	}

	public String validate() {
		
		if (table==null || table.equals("")) return "nome tabella necessario";
		if (db   ==null || db   .equals("")) return "nome db necessario";
		if ( kv.size() < 1 ) return "Questa classe non ha parametri, non puo' essere trasformata";

		
		if (! table.matches(BASE_REGEX)) return " nome tabella "+table+" non valido";
		if (! db   .matches(BASE_REGEX)) return " nome db "+db+" non valido";
		
		if (! (table+suffix).matches(BASE_REGEX) ) return " suffisso "+ suffix +" scelto non valido";
		if (! (prefix+table).matches(BASE_REGEX) ) return " prefisso "+ prefix +" scelto non valido";
		
		for (Entry<String,String> kv : kv.entrySet()) {
			if (kv.getKey()  == null || kv.getKey().equals("") ) return "Una colonna \u00e8 stata trovata vuota";
			if ( ! kv.getKey().matches(BASE_REGEX) ) return "La colonna "+kv.getKey()+" non \u00e8 valida";
		}
		
		return "";
	}
	
	
	public String create() {
		
		StringBuilder sb=new StringBuilder(kv.size()*20);
		
		String validation=validate();
		
		if(!validation.equals("")) throw new IllegalArgumentException(validation);
		
		sb.append("create table `"+db+"`.`"+attachPreSuf(table)+"` (");
		
		for (Entry<String,String> kv :this.kv.entrySet() ) {
			sb.append(attachPreSuf(kv.getKey())+' '+kv.getValue());
			sb.append(primary.contains(kv.getKey())? " primary key," : ",");
		}
		
		sb.setCharAt(sb.length()-1, ')');
		return sb.toString();
	}

	@Override
	public String select() {
		String validation=validate();
		
		if(!validation.equals("")) throw new IllegalArgumentException(validation);
		
		String sb="select * from information_schema where table_schema='"+db+"' and table_name='"+attachPreSuf(table)+"'";
		
		return sb;
	}

	@Override
	public String drop() {
		String validation=validate();
		
		if(!validation.equals("")) throw new IllegalArgumentException(validation);
		
		String sb="drop table if exists `"+db+"`.`"+attachPreSuf(table)+"`";
		
		return sb;
	}

	@Override
	public QueryCraft insertData(Object o) {
		QueryCraft qc=new SQLInsertCraft().DB(db).table(table);
		
		Map<String,Object> map=SQLClassParser.parseInstance(type, o);
		
		for (Entry<String,Object> entry : map.entrySet()) {
			if(entry.getValue()==null) continue;
			qc.entry(entry);
		}
		
		return qc;
	}

	@Override
	public SelectCraft selectData(Object o) {
		SelectCraft qc=new SQLSelectCraft().DB(db).table(table);
		
		Map<String,Object> map=SQLClassParser.parseInstance(type, o);
		
		for (Entry<String,Object> entry : map.entrySet()) {
			if(entry.getValue()==null) continue;
			qc.filter(entry);
		}
		
		return qc;
	}

	@Override
	public QueryCraft deleteData(Object o) {
		QueryCraft qc=new SQLDeleteCraft().DB(db).table(table);
		
		Map<String,Object> map=SQLClassParser.parseInstance(type, o);
		
		for (Entry<String,Object> entry : map.entrySet()) {
			if(entry.getValue()==null) continue;
			qc.filter(entry);
		}
		
		return qc;
	}

	@Override
	public QueryCraft updateData(Object o) {
		if (primary.isEmpty())
			throw new IllegalArgumentException("deve essere presente almeno una chiave primaria");
		
		QueryCraft qc=new SQLUpdateCraft().DB(db).table(table);
		
		Map<String,Object> map=SQLClassParser.parseInstance(type, o);
		
		for (Entry<String,Object> entry : map.entrySet()) {
			if(primary.contains(entry.getKey()) ) {
				if( entry.getValue()==null  ) 
					throw new IllegalArgumentException("Gli elementi nella chiave primaria non possono essere null");
				
				qc.filter(entry);
			} else {
				if(entry.getValue()==null) continue;
				
				qc.entry(entry);
			}
		}
		
		return qc;
	}
	
}
