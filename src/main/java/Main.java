import org.apache.commons.cli.*;

public class Main {
    private static final CommandLineParser parser = new DefaultParser();
    private static final HelpFormatter formatter = new HelpFormatter();
    private static final int MAX_CHAIN_LENGTH = 1000000;
    private static final int MAX_N_CHAINS = 100000000;
    private static final int MAX_N_THREADS = 1024;

    private enum NumberArgType {
        chainLength,
        nChains,
        nThreads
    }

    public static void main(String[] args) {
        Main main = new Main();

        Options options = new Options();
        Option password = new Option("p", "password", true, "hasło do zaszyfrowania (dowolny ciąg znaków bez spacji)");
        Option secretKey = new Option("sk", "secretKey", true, "klucz o długości dokładnie " + DES.KEY_LENGTH + " bajtów (" + DES.KEY_LENGTH + " cyfr z zakresu 0-9)");
        Option cipherText = new Option("c", "cipherText", true, "kryptogram (zaszyfrowane hasło w postaci szesnastkowej)");
        Option file = new Option("f", "file", true, "nazwa pliku z tablicą tęczową");
        Option chainLength = new Option("cl", "chainLength", true, "liczba kluczy w każdym łańcuchu");
        Option nChains = new Option("nc", "nChains", true, "[opcjonalne] liczba łańcuchów do wygenerowania; jeśli nie będzie podana, zostanie użyta domyślna wartość");
        Option nThreads = new Option("nt", "nThreads", true, "[opcjonalne] liczba wątków, domyślnie równa ilości rdzeni");

        String argMode, argPassword, argSecretKey, argCipherText, argFile, argChainLength, argNChains, argNThreads;

        Option mode = new Option("m", "mode", true, "tryb działania programu: [encrypt, decrypt, generate, crack]");
        mode.setRequired(true);
        options.addOption(mode);

        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args, true);
        } catch (ParseException e) {
            formatter.printHelp("java -jar rtdes.jar", options);
            System.exit(1);
        }

        argMode = cmd.getOptionValue("mode");

        switch (argMode) {
            case "encrypt":
                password.setRequired(true);
                options.addOption(password);

                secretKey.setRequired(false);
                secretKey.setDescription("[opcjonalne] klucz o długości dokładnie " + DES.KEY_LENGTH + " bajtów (" + DES.KEY_LENGTH + " cyfr z zakresu 0-9), którym ma zostać zaszyfrowane hasło; jeśli nie będzie podany, zostanie wygenerowany losowo");
                options.addOption(secretKey);

                cmd = parseArgs(options, args);

                argPassword = cmd.getOptionValue("password");
                argSecretKey = cmd.getOptionValue("secretKey");

                try {
                    main.encrypt(argPassword, argSecretKey);
                } catch (IllegalArgumentException e) {
                    System.err.println(e.getMessage());
                }
                break;
            case "decrypt":
                cipherText.setRequired(true);
                options.addOption(cipherText);

                secretKey.setRequired(true);
                secretKey.setDescription("klucz o długości dokładnie " + DES.KEY_LENGTH + " bajtów (" + DES.KEY_LENGTH + " cyfr z zakresu 0-9), którym zostało zaszyfrowane hasło");
                options.addOption(secretKey);

                cmd = parseArgs(options, args);

                argCipherText = cmd.getOptionValue("cipherText");
                argSecretKey = cmd.getOptionValue("secretKey");

                try {
                    main.decrypt(argCipherText, argSecretKey);
                } catch (IllegalArgumentException e) {
                    System.err.println(e.getMessage());
                }
                break;
            case "generate":
                file.setRequired(true);
                file.setDescription("nazwa pliku, do którego ma zostać zapisana tablica tęczowa");
                options.addOption(file);

                chainLength.setRequired(true);
                options.addOption(chainLength);

                password.setRequired(true);
                options.addOption(password);

                nChains.setRequired(false);
                options.addOption(nChains);

                nThreads.setRequired(false);
                options.addOption(nThreads);

                cmd = parseArgs(options, args);

                argFile = cmd.getOptionValue("file");
                argChainLength = cmd.getOptionValue("chainLength");
                argPassword = cmd.getOptionValue("password");
                argNChains = cmd.getOptionValue("nChains");
                argNThreads = cmd.getOptionValue("nThreads");

                main.generate(argFile, argChainLength, argPassword, argNChains, argNThreads);
                break;
            case "crack":
                file.setRequired(true);
                file.setDescription("nazwa pliku zawierającego tablicę tęczową");
                options.addOption(file);

                cipherText.setRequired(true);
                options.addOption(cipherText);

                nThreads.setRequired(false);
                options.addOption(nThreads);

                cmd = parseArgs(options, args);

                argFile = cmd.getOptionValue("file");
                argCipherText = cmd.getOptionValue("cipherText");
                argNThreads = cmd.getOptionValue("nThreads");

                main.crack(argFile, argCipherText, argNThreads);
                break;
            default:
                System.err.println("Nieznany tryb programu. Dostępne tryby: encrypt, decrypt, generate, crack");
        }
    }

    private static CommandLine parseArgs(Options options, String[] args) {
        try {
            return parser.parse(options, args, true);
        } catch (ParseException e) {
            formatter.printHelp("java -jar rtdes.jar", options);
            System.exit(1);
        }

        return null;
    }

    private int parseNumberString(String numberString, NumberArgType type) {
        int number = Integer.MAX_VALUE;
        boolean numberOutOfRange = false;

        try {
            number = Integer.parseInt(numberString);
            switch (type) {
                case chainLength:
                    if (number < 1 || number > MAX_CHAIN_LENGTH) {
                        System.err.println("Długość łańcucha musi być pomiędzy 1 a " + MAX_CHAIN_LENGTH);
                        numberOutOfRange = true;
                    }
                    break;
                case nChains:
                    if (number < 1 || number > MAX_N_CHAINS) {
                        System.err.println("Liczba łańcuchów musi być pomiędzy 1 a " + MAX_N_CHAINS);
                        numberOutOfRange = true;
                    }
                    break;
                case nThreads:
                    if (number < 1 || number > MAX_N_THREADS) {
                        System.err.println("Liczba wątków musi być pomiędzy 1 a " + MAX_N_THREADS);
                        numberOutOfRange = true;
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            System.err.println("Błędny format liczby " + type);
        }

        if (number == Integer.MAX_VALUE || numberOutOfRange) {
            System.exit(-1);
        }

        return number;
    }

    private void encrypt(String password, String secretKey) throws IllegalArgumentException {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Hasło nie może być puste");
        }

        if (secretKey == null || secretKey.isEmpty()) {
            secretKey = DES.generateRandomKey();
            System.out.println("Do zaszyfrowania użyto losowego klucza: " + secretKey);
        }

        DES des = new DES();
        des.initializeEncryptor(secretKey);
        String hexCipherText = des.encrypt(password);
        System.out.println("Kryptogram: " + hexCipherText);
    }

    private void decrypt(String cipherText, String secretKey) throws IllegalArgumentException {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("Klucz nie może być pusty");
        }

        DES des = new DES();
        des.initializeDecryptor(secretKey);
        String hexPlainText = des.decrypt(cipherText);
        System.out.println("Odszyfrowane hasło: " + new String(Hex.hexStringToByteArray(hexPlainText)));
    }

    private void generate(String argFile, String argChainLength, String argPassword, String argNChains, String argNThreads) {
        int chainLength = 1000;
        int nChains = 1000;
        int nThreads = Runtime.getRuntime().availableProcessors();

        if (argChainLength != null && !argChainLength.isEmpty()) {
            chainLength = parseNumberString(argChainLength, NumberArgType.chainLength);
        }
        if (argNChains != null && !argNChains.isEmpty()) {
            nChains = parseNumberString(argNChains, NumberArgType.nChains);
        }
        if (argNThreads != null && !argNThreads.isEmpty()) {
            nThreads = parseNumberString(argNThreads, NumberArgType.nThreads);
        }

        RainbowTable rainbowTable = new RainbowTableVerbose(DES.KEY_LENGTH, chainLength, argPassword);
        try {
            rainbowTable.generate(nChains, nThreads);
            rainbowTable.saveToFile(argFile);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    private void crack(String argFile, String argCipherText, String argNThreads) {
        int nThreads = Runtime.getRuntime().availableProcessors();

        if (argNThreads != null && !argNThreads.isEmpty()) {
            nThreads = parseNumberString(argNThreads, NumberArgType.nThreads);
        }

        RainbowTable rainbowTable;

        try {
            rainbowTable = RainbowTableVerbose.readFromFile(argFile);
        } catch (Exception e) {
            System.err.println("Błąd podczas wczytywania tablicy z pliku: " + e.getMessage());
            return;
        }

        System.out.println("Wczytano tablicę: liczba łańcuchów = " + rainbowTable.getTableSize()
                + ", długość łańcucha = " + rainbowTable.getChainLength() + ", plaintext = " + rainbowTable.getPlaintext());

        byte[] foundKey = rainbowTable.lookup(argCipherText, nThreads);
        if (foundKey != null) {
            System.out.println("Znaleziono klucz: " + new String(foundKey));
        } else {
            System.out.println("Nie znaleziono klucza");
        }
    }
}
