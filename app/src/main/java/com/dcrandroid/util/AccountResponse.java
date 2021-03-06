package com.dcrandroid.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by collins on 12/30/17.
 */
public class AccountResponse {
    public boolean errorOccurred;
    public String errorMessage, currentBlockHash;
    public int count, errorCode, currentBlockHeight;
    public ArrayList<AccountItem> items = new ArrayList<>();
    public final static float SATOSHI = 100000000;
    private AccountResponse(){}
    public static AccountResponse parse(String json) throws JSONException {
        //System.out.println("Account JSON: "+json);
        AccountResponse response = new AccountResponse();
        JSONObject obj = new JSONObject(json);
        response.errorOccurred = obj.getBoolean("ErrorOccurred");
        response.errorMessage = obj.getString("ErrorMessage");
        response.errorCode = obj.getInt("ErrorCode");
        if(!response.errorOccurred) {
            response.count = obj.getInt("Count");
            response.currentBlockHeight = obj.getInt("CurrentBlockHeight");
            response.currentBlockHash = obj.getString("CurrentBlockHash");
            JSONArray acc = obj.getJSONArray("Acc");
            for (int i = 0; i < acc.length(); i++) {
                final JSONObject account = acc.getJSONObject(i);
                response.items.add(new AccountItem() {
                    {
                        number = account.getInt("Number");
                        name = account.getString("Name");
                        System.out.println();
                        externalKeyCount = account.getInt("ExternalKeyCount");
                        internalKeyCount = account.getInt("InternalKeyCount");
                        importedKeyCount = account.getInt("ImportedKeyCount");
                        JSONObject balanceObj = account.getJSONObject("Balance");
                        balance = new Balance();
                        balance.total = balanceObj.getLong("Total");
                        balance.spendable = balanceObj.getLong("Spendable");
                        balance.immatureReward = balanceObj.getLong("ImmatureReward");
                        balance.immatureStakeGeneration = balanceObj.getLong("ImmatureStakeGeneration");
                        balance.lockedByTickets = balanceObj.getLong("LockedByTickets");
                        balance.votingAuthority = balanceObj.getLong("VotingAuthority");
                        balance.unConfirmed = balanceObj.getLong("UnConfirmed");
                    }
                });
            }
        }
        return response;
    }
    public static class AccountItem{
        public int number, externalKeyCount,internalKeyCount,importedKeyCount;
        public String name;
        public Balance balance;
    }
    public static class Balance{
        public long spendable, total,immatureReward, immatureStakeGeneration,lockedByTickets, votingAuthority, unConfirmed;
    }
}