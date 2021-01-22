package psykeco.querycraft.err.dict;

import static psykeco.querycraft.err.ErrMsg.KEY_MSG;
import static psykeco.querycraft.err.ErrMsg.KEY_MSG.*;

import java.util.HashMap;

public final class ITErrMsg {
	
	public static HashMap<KEY_MSG, String> getDict(){
		HashMap<KEY_MSG, String> dict=new HashMap<>();
		
		dict.put(PORT_NOT_VALID, "valore della porta errato");
		dict.put(URL_NULL, "url vuoto");
		dict.put(USER_NULL, "utente non valido");
		dict.put(PSK_NULL, "psk non valida");
		dict.put(DB_NULL, "nome db necessario");
		dict.put(DB_NOT_VALID, "nome db %s non valido");
		dict.put(TABLE_NULL, "nome tabella necessario");
		dict.put(TABLE_NOT_VALID, " nome tabella %s non valido");
		dict.put(COLUMN_EMPTY, "Una colonna \u00e8 stata trovata vuota");
		dict.put(VALUE_EMPTY, "Il valore di %s \u00e8 stata trovata vuota");
		dict.put(COLUMN_NOT_VALID, "La colonna %s non \u00e8 valida");
		dict.put(VALUE_NOT_VALID, "Il valore %s non \u00e8 valido");
		dict.put(ENTRY_EMPTY, "lista entry vuota. Serve almeno una coppia colonna-valore");
		dict.put(NOT_SUPPORT, "%s non supporta i %s");
		dict.put(CLASS_PARAMETERS, "Questa classe non ha parametri, non puo' essere trasformata");
		dict.put(PREFIX_NOT_VALID, "prefisso %s scelto non valido");
		dict.put(SUFFIX_NOT_VALID, "suffisso %s scelto non valido");
		dict.put(PRIMARY_NOT_NULL, "Gli elementi nella chiave primaria non possono essere null");
		
		//dict.put(, "");
		return dict;		
	}
	

}