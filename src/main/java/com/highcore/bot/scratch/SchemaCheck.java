
package com.highcore.bot.scratch;
import com.highcore.bot.database.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SchemaCheck {
    public static void main(String[] args) {
        JsonArray arr = SupabaseClient.get("dc_vouchers", "limit=1");
        if (arr != null && arr.size() > 0) {
            JsonObject obj = arr.get(0).getAsJsonObject();
            System.out.println("Voucher Keys: " + obj.keySet());
        } else {
            System.out.println("No records found in dc_vouchers");
        }
    }
}
