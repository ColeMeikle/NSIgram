package com.example.application.ce.webpages.backend;

public class PasswordConditions {
    private static final int MIN_PWD_LENGTH = 8;

    public static String CheckPwdConditions(String pwd){

        if(pwd.length() < MIN_PWD_LENGTH){
            return "Password must be at least 8 characters long";
        }
        if(pwd.indexOf(' ') != -1){
            return "Password may not contain whitespace";
        }

        boolean hasLowercase = false;
        boolean hasUppercase = false;
        boolean hasDigit = false;

        for (int i = 0; i < pwd.length(); i++) {
            Character c = pwd.charAt(i);
            if (Character.isDigit(c)){
                hasDigit = true;
            } else{
                if (Character.isUpperCase(c)) {
                    hasUppercase = true;
                }
                if (Character.isLowerCase(c)){
                    hasLowercase = true;
                }
            }
        }

        if(!hasLowercase){
            return "Password must contain a lowercase character";
        }
        if(!hasUppercase){
            return "Password must contain an uppercase character";
        }
        if(!hasDigit){
            return "Password must contain a digit";
        }
        return "";
    }
}
