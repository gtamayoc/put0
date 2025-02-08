package gtc.dcc.put0.core.utils;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.zip.CRC32;

public class CodeGenerator {

    private final String format;
    private final Random random = new Random();

    public CodeGenerator(String format) {
        this.format = format;
    }

    public String generateCode() {
        StringBuilder code = new StringBuilder();

        for (char c : format.toCharArray()) {
            if (c == '#') {
                code.append(random.nextInt(10)); // Números
            } else if (c == '@') {
                code.append((char) ('A' + random.nextInt(26))); // Letras mayúsculas
            } else {
                code.append(c); // Caracter fijo
            }
        }

        return code.toString();
    }

    public int generateUniqueId1(String gameName, String gameCode) {
        // Obtener el tiempo actual en milisegundos
        long timestamp = System.currentTimeMillis();

        // Crear una combinación única
        String uniqueInput = gameName + gameCode + timestamp;

        // Generar un hash de 32 bits usando CRC32
        CRC32 crc = new CRC32();
        crc.update(uniqueInput.getBytes(StandardCharsets.UTF_8));

        // Convertir el hash (long) a int dentro del rango permitido
        long hash = crc.getValue();
        int identifier = (int) (hash % Integer.MAX_VALUE);

        return identifier;
    }

    // Generar un identificador único usando CRC32
    public int generateUniqueId(String gameName, String gameCode) {
        long timestamp = System.currentTimeMillis(); // Añade un componente único basado en el tiempo
        String uniqueInput = gameName + gameCode + timestamp; // Combina los datos para generar un hash

        CRC32 crc = new CRC32();
        crc.update(uniqueInput.getBytes(StandardCharsets.UTF_8));

        long hash = crc.getValue();
        return (int) (hash & 0x7FFFFFFF); // Convertir a un entero positivo de 32 bits
    }

}