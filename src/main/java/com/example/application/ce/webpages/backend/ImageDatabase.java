package com.example.application.ce.webpages.backend;


import java.io.InputStream;

public class ImageDatabase {

    public static void uploadImage(String currentUser, String fileName, InputStream fileData){
        try {
            String url = "https://zxrwcyxrtjcobknxopsb.supabase.co/storage/v1/object/profile-pictures/" + fileName;

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp4cndjeXhydGpjb2JrbnhvcHNiIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0MzU1MTcxMSwiZXhwIjoyMDU5MTI3NzExfQ.3qrvTRl6e5x-owPJOqkkaZN-BdOfRusS5NuOgEMWHQc"); // Replace with actual key
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setDoOutput(true);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                fileData.transferTo(os);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200 && responseCode != 201) {
                throw new java.io.IOException("Failed to upload: HTTP " + responseCode);
            }

            UserManagement.updateUserProfilePicture(currentUser, fileName);

            System.out.println("Uploaded to Supabase successfully.");

        } catch (java.io.IOException e) {
            System.err.println("Upload failed: " + e.getMessage());
        }
    }
}
