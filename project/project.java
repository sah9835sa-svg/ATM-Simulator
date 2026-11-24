
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class project {

    private static final Path ACCOUNT_FILE = Paths.get("accounts.txt");
    private static final int[] NOTES = {500, 200, 100};
    private static final int MAX_PIN_ATTEMPTS = 3;

    private static class Account {
        final String number;
        final String name;
        final String pin;
        int balance;
        boolean blocked;

        final Deque<String> transactions = new ArrayDeque<>();

        Account(String number, String name, String pin,
                int balance, boolean blocked) {
            this.number = number;
            this.name = name;
            this.pin = pin;
            this.balance = balance;
            this.blocked = blocked;
        }
    }

    private final Map<String, Account> accounts = new HashMap<>();
    private final Deque<Account> waitingCustomers = new ArrayDeque<>();

    private Account activeAccount;

    public static void main(String[] args) {
        new project().run();
    }

    private void run() {

        loadAccounts();

        if (accounts.isEmpty()) {
            System.out.println("No accounts available. Check accounts.txt.");
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {

            System.out.println("\n===== ATM SIMULATOR =====");
            System.out.println("Waiting customers: " + waitingCustomers.size());

            boolean running = true;

            while (running) {

                printMenu();

                String choice = readLine(scanner, "Choose an option: ");

                switch (choice) {

                    case "1":
                        serveNextCustomer(scanner);
                        break;

                    case "2":
                        showBalance();
                        break;

                    case "3":
                        deposit(scanner);
                        break;

                    case "4":
                        withdraw(scanner);
                        break;

                    case "5":
                        showMiniStatement();
                        break;

                    case "6":
                        logout();
                        break;

                    case "7":
                        running = false;
                        break;

                    default:
                        System.out.println("Invalid menu option.");
                }
            }

        }

        saveAccounts();

        System.out.println("Account data saved. Goodbye.");
    }

    private void printMenu() {

        System.out.println("\n========== ATM MENU ==========");
        System.out.println("1. Serve next customer");
        System.out.println("2. Check balance");
        System.out.println("3. Deposit");
        System.out.println("4. Withdraw");
        System.out.println("5. Mini statement");
        System.out.println("6. Logout");
        System.out.println("7. Exit");
        System.out.println("==============================");
    }

    private void serveNextCustomer(Scanner scanner) {

        if (activeAccount != null) {
            System.out.println("Please logout before serving another customer.");
            return;
        }

        if (waitingCustomers.isEmpty()) {
            System.out.println("No customers are waiting.");
            return;
        }

        Account candidate = waitingCustomers.peek();

        if (candidate.blocked) {
            waitingCustomers.remove();
            System.out.println("Card blocked. Customer removed from queue.");
            return;
        }

        for (int attempts = 0; attempts < MAX_PIN_ATTEMPTS; attempts++) {

            String enteredPin = readLine(scanner, "Enter PIN: ");

            if (candidate.pin.equals(enteredPin)) {

                waitingCustomers.remove();
                activeAccount = candidate;

                System.out.println("\nLogin successful!");
                System.out.println("Welcome, " + candidate.name);
                System.out.println("Balance: " + candidate.balance);

                return;
            }

            int left = MAX_PIN_ATTEMPTS - attempts - 1;

            if (left == 0) {

                candidate.blocked = true;
                waitingCustomers.remove();

                System.out.println("Incorrect PIN.");
                System.out.println("Card blocked after 3 attempts.");

            } else {

                System.out.println("Incorrect PIN.");
                System.out.println("Attempts remaining: " + left);
            }
        }
    }

    private void showBalance() {

        if (!requireLogin()) return;

        System.out.println("Available balance: " + activeAccount.balance);
    }

    private void deposit(Scanner scanner) {

        if (!requireLogin()) return;

        Integer amount = readAmount(scanner, "Enter deposit amount: ");

        if (amount == null) return;

        if (amount <= 0 || amount % 100 != 0) {
            System.out.println("Amount must be a positive multiple of 100.");
            return;
        }

        if (activeAccount.balance > Integer.MAX_VALUE - amount) {
            System.out.println("Balance limit exceeded.");
            return;
        }

        activeAccount.balance += amount;

        recordTransaction(
            "Deposit: " + amount +
            " | Balance: " + activeAccount.balance
        );

        System.out.println("Deposit successful!");
        System.out.println("New balance: " + activeAccount.balance);
    }

    private void withdraw(Scanner scanner) {

        if (!requireLogin()) return;

        Integer amount = readAmount(scanner, "Enter withdrawal amount: ");

        if (amount == null) return;

        if (amount <= 0 || amount % 100 != 0) {
            System.out.println("Amount must be a positive multiple of 100.");
            return;
        }

        if (amount > activeAccount.balance) {
            System.out.println("Insufficient balance.");
            return;
        }

        int remaining = amount;

        List<String> dispensed = new ArrayList<>();

        for (int note : NOTES) {

            int count = remaining / note;

            if (count > 0) {
                dispensed.add(note + " x " + count);
            }

            remaining %= note;
        }

        if (remaining != 0) {
            System.out.println("Unable to dispense this amount.");
            return;
        }

        activeAccount.balance -= amount;

        recordTransaction(
            "Withdraw: " + amount +
            " | Balance: " + activeAccount.balance
        );

        System.out.println("Withdrawal successful!");
        System.out.println("Dispensed notes: " + String.join(", ", dispensed));
        System.out.println("Remaining balance: " + activeAccount.balance);
    }

    private void showMiniStatement() {

        if (!requireLogin()) return;

        System.out.println("\n===== MINI STATEMENT =====");

        if (activeAccount.transactions.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }

        int index = 1;

        for (String transaction : activeAccount.transactions) {
            System.out.println(index++ + ". " + transaction);
        }
    }

    private void recordTransaction(String transaction) {

        activeAccount.transactions.push(transaction);
    }

    private void logout() {

        if (!requireLogin()) return;

        System.out.println("Thank you, " + activeAccount.name + ".");
        System.out.println("You have successfully logged out.");

        activeAccount = null;
    }

    private boolean requireLogin() {

        if (activeAccount == null) {
            System.out.println("Please login first.");
            return false;
        }

        return true;
    }

    private Integer readAmount(Scanner scanner, String prompt) {

        String input = readLine(scanner, prompt);

        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            System.out.println("Please enter a valid whole-number amount.");
            return null;
        }
    }

    private String readLine(Scanner scanner, String prompt) {

        System.out.print(prompt);

        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }

        return "7";
    }

    private void loadAccounts() {

        if (!Files.exists(ACCOUNT_FILE)) {
            System.out.println("accounts.txt not found.");
            return;
        }

        try {

            int lineNumber = 0;

            for (String rawLine :
                    Files.readAllLines(ACCOUNT_FILE, StandardCharsets.UTF_8)) {

                lineNumber++;

                String line = rawLine.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] fields = line.split(",", -1);

                if (lineNumber == 1 &&
                        fields[0].trim().equalsIgnoreCase("accountNumber")) {
                    continue;
                }

                if (fields.length != 5) {
                    System.out.println("Malformed line: " + lineNumber);
                    continue;
                }

                try {

                    String number = fields[0].trim();
                    String name = fields[1].trim();
                    String pin = fields[2].trim();
                    int balance = Integer.parseInt(fields[3].trim());
                    String blockedValue = fields[4].trim();

                    if (number.isEmpty() ||
                            name.isEmpty() ||
                            pin.isEmpty() ||
                            balance < 0 ||
                            (!blockedValue.equalsIgnoreCase("true") &&
                             !blockedValue.equalsIgnoreCase("false")) ||
                            accounts.containsKey(number)) {

                        System.out.println("Invalid account on line " + lineNumber);
                        continue;
                    }

                    Account account = new Account(
                        number,
                        name,
                        pin,
                        balance,
                        Boolean.parseBoolean(blockedValue)
                    );

                    accounts.put(number, account);
                    waitingCustomers.add(account);

                } catch (NumberFormatException exception) {

                    System.out.println("Invalid balance on line " + lineNumber);
                }
            }

        } catch (IOException exception) {

            System.out.println("Could not load accounts.txt: " +
                    exception.getMessage());
        }
    }

    private void saveAccounts() {

        List<String> lines = new ArrayList<>();

        lines.add("accountNumber,name,pin,balance,blocked");

        List<String> numbers = new ArrayList<>(accounts.keySet());

        Collections.sort(numbers);

        for (String number : numbers) {

            Account account = accounts.get(number);

            lines.add(
                String.join(",",
                    account.number,
                    account.name,
                    account.pin,
                    Integer.toString(account.balance),
                    Boolean.toString(account.blocked)
                )
            );
        }

        try {

            Files.write(
                ACCOUNT_FILE,
                lines,
                StandardCharsets.UTF_8
            );

        } catch (IOException exception) {

            System.out.println("Could not save accounts.txt: " +
                    exception.getMessage());
        }
    }
}
