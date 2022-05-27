import org.apache.commons.cli.*;

public class Main {
    private static final CommandLineParser parser = new DefaultParser();
    private static final HelpFormatter formatter = new HelpFormatter();

    public static void main(String[] args) {
        Main main = new Main();

        Options options = new Options();
        Option password = new Option("p", "password", true, "hasło do zaszyfrowania (dowolny ciąg znaków bez spacji)");
        Option secretKey = new Option("sk", "secretKey", true, "klucz o długości dokładnie 8 bajtów (8 cyfr z zakresu 0-9)");
        Option ciphertext = new Option("c", "ciphertext", true, "kryptogram (zaszyfrowane hasło w postaci szesnastkowej)");
        Option file = new Option("f", "file", true, "nazwa pliku z tablicą tęczową");
        Option chainLength = new Option("cl", "chainLength", true, "liczba kluczy w każdym łańcuchu");
        Option nChains = new Option("nc", "nChains", true, "[opcjonalne] liczba łańcuchów do wygenerowania; jeśli nie będzie podana, zostanie użyta domyślna wartość");
        Option nThreads = new Option("nt", "nThreads", true, "[opcjonalne] liczba wątków, domyślnie równa ilości rdzeni");

        String argMode, argPassword, argSecretKey, argCiphertext, argFile, argChainLength, argNChains, argNThreads;

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
                secretKey.setDescription("[opcjonalne] klucz o długości dokładnie 8 bajtów (8 cyfr z zakresu 0-9), którym ma zostać zaszyfrowane hasło; jeśli nie będzie podany, zostanie wygenerowany losowo");
                options.addOption(secretKey);

                cmd = parseArgs(options, args);

                argPassword = cmd.getOptionValue("password");
                argSecretKey = cmd.getOptionValue("secretKey");

                main.encrypt(argPassword, argSecretKey);
                break;
            case "decrypt":
                ciphertext.setRequired(true);
                options.addOption(ciphertext);

                secretKey.setRequired(true);
                secretKey.setDescription("klucz o długości dokładnie 8 bajtów (8 cyfr z zakresu 0-9), którym zostało zaszyfrowane hasło");
                options.addOption(secretKey);

                cmd = parseArgs(options, args);

                argCiphertext = cmd.getOptionValue("ciphertext");
                argSecretKey = cmd.getOptionValue("secretKey");

                main.decrypt(argCiphertext, argSecretKey);
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

                ciphertext.setRequired(true);
                options.addOption(ciphertext);

                nThreads.setRequired(false);
                options.addOption(nThreads);

                cmd = parseArgs(options, args);

                argFile = cmd.getOptionValue("file");
                argCiphertext = cmd.getOptionValue("ciphertext");
                argNThreads = cmd.getOptionValue("nThreads");

                main.crack(argFile, argCiphertext, argNThreads);
                break;
            default:
                // TODO
        }

        /*
        String charset = "abctajxyzne";
        int passwordLength = 5;
        int chainLength = 1000;
        int numChains = 140; // set by trial and error, higher values may cause infinite(?) generation
        String pathname = "table.txt";
        DES des = new DES();

        RainbowTable rainbowTable = new RainbowTableVerbose(charset, passwordLength, chainLength, des);
        String s = "Starting rainbow table generation:\n" +
                "password length: " + passwordLength + "\n" +
                "charset: " + charset + "\n" +
                "chains: " + numChains + "\n" +
                "reductions for each chain: " + chainLength + "\n";
        System.out.println(s);

        try {
            rainbowTable.generate(numChains, 4);
        } catch (InterruptedException ignored) {}
        double saveSeconds = rainbowTable.saveTableToFile(pathname);
        System.out.println("Table saved to file \"" + pathname + "\" in " + saveSeconds + "s\n");

        String cipherTextToCrack, foundPass;

        try {
            cipherTextToCrack = des.encrypt("tajne");
            foundPass = rainbowTable.lookup(cipherTextToCrack);
            if (foundPass != null) {
                System.out.println("For cipherText: " + cipherTextToCrack + " found password: " + foundPass);
            } else {
                System.out.println("Rainbow table doesn't contain the password for given cipherText: " + cipherTextToCrack);
            }
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }

         */
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

    private void encrypt(String argPassword, String argSecretKey) {
        System.out.println("Wywołanie encrypt(" + argPassword + ", " + argSecretKey + ")");
    }

    private void decrypt(String argCiphertext, String argSecretKey) {
    }

    private void generate(String argFile, String argChainLength, String argPassword, String argNChains, String argNThreads) {
    }

    private void crack(String argFile, String argCiphertext, String argNThreads) {
    }
}
