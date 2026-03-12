package com.example.demo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LoginManager {

    private final String filePath = "src/main/resources/users.txt";

    // regis
    public boolean idExists(String id) {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(","); // ID,Name,Email,Phone,Gender,Password,Type
                if (parts[0].equals(id)) {
                    return true; // ID exists
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false; // ID not found
    }

    // set pass
    public boolean setPassword(String id, String password) {
        try {
            File file = new File(filePath);
            List<String> lines = new ArrayList<>();
            boolean updated = false;

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts[0].equals(id)) {
                        if (parts.length < 6) {
                            String[] temp = new String[6];
                            System.arraycopy(parts, 0, temp, 0, parts.length);
                            parts = temp;
                        }
                        parts[5] = password; // set the password
                        updated = true;
                    }
                    lines.add(String.join(",", parts));
                }
            }

            if (updated) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                    for (String l : lines) {
                        bw.write(l);
                        bw.newLine();
                    }
                }
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // login check
    public boolean validateLogin(String id, String password) {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals(id) && parts.length >= 6 && parts[5].equals(password)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // seeker or provider stuff
    public String getUserType(String id) {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals(id) && parts.length >= 7) {
                    return parts[6];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "seeker";
    }

    // get name
    public String getUserName(String id) {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals(id) && parts.length >= 2) {
                    return parts[1]; // return the Name
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ""; // if not found
    }
}
