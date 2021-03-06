package com.dcrandroid.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dcrandroid.MainActivity;
import com.dcrandroid.activities.TransactionDetailsActivity;
import com.dcrandroid.adapter.TransactionAdapter;
import com.dcrandroid.R;

import com.dcrandroid.data.Constants;
import com.dcrandroid.util.AccountResponse;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.RecyclerTouchListener;
import com.dcrandroid.util.TransactionSorter;
import com.dcrandroid.util.TransactionsResponse;
import com.dcrandroid.data.Transaction;
import com.dcrandroid.util.Utils;
import com.dcrandroid.view.CurrencyTextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import mobilewallet.GetTransactionsResponse;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class OverviewFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, GetTransactionsResponse {
    private List<Transaction> transactionList = new ArrayList<>(), tempTxList = new ArrayList<>();
    private CurrencyTextView tvBalance;
    private SwipeRefreshLayout swipeRefreshLayout;

    TransactionAdapter transactionAdapter;
    TextView refresh;
    PreferenceUtil util;
    RecyclerView recyclerView;
    DcrConstants constants;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(getContext() == null){
            return null;
        }
        util = new PreferenceUtil(getContext());
        constants = DcrConstants.getInstance();
        View rootView = inflater.inflate(R.layout.content_overview, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        swipeRefreshLayout = rootView.getRootView().findViewById(R.id.swipe_refresh_layout2);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(this);
        recyclerView = rootView.getRootView().findViewById(R.id.history_recycler_view2);
        refresh = rootView.getRootView().findViewById(R.id.no_history);
        transactionAdapter = new TransactionAdapter(transactionList, layoutInflater);
        tvBalance = rootView.getRootView().findViewById(R.id.overview_av_balance);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration( getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Transaction history = transactionList.get(position);
                Intent i = new Intent(getContext(), TransactionDetailsActivity.class);
                i.putExtra(Constants.EXTRA_BLOCK_HEIGHT, history.getHeight());
                i.putExtra(Constants.EXTRA_AMOUNT,history.getAmount());
                i.putExtra(Constants.EXTRA_TRANSACTION_FEE,history.getTransactionFee());
                i.putExtra(Constants.EXTRA_TRANSACTION_DATE,history.getTxDate());
                i.putExtra(Constants.EXTRA_TRANSACTION_TYPE,history.getType());
                i.putExtra(Constants.EXTRA_TRANSACTION_TOTAL_INPUT, history.totalInput);
                i.putExtra(Constants.EXTRA_TRANSACTION_TOTAL_OUTPUT, history.totalOutput);
                i.putExtra(Constants.EXTRA_TRANSACTION_HASH, history.getHash());
                i.putExtra(Constants.EXTRA_TRANSACTION_DIRECTION, history.getDirection());
                i.putStringArrayListExtra(Constants.EXTRA_TRANSACTION_INPUTS,history.getUsedInput());
                i.putStringArrayListExtra(Constants.EXTRA_TRANSACTION_OUTPUTS,history.getWalletOutput());
                startActivity(i);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        TextView showHistory= rootView.findViewById(R.id.show_history);
        showHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMainA();
            }
        });

        recyclerView.setAdapter(transactionAdapter);
        registerForContextMenu(recyclerView);
        prepareHistoryData();
        getBalance();
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle(getString(R.string.overview));
    }

    private void getBalance(){
        new Thread(){
            public void run(){
                try {
                    if(getContext() == null){
                        return;
                    }
                    final AccountResponse response = AccountResponse.parse(constants.wallet.getAccounts(util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS));
                    long totalBalance = 0;
                    for(int i = 0; i < response.items.size(); i++){
                        AccountResponse.Balance balance = response.items.get(i).balance;
                        totalBalance += balance.total;
                    }
                    final long finalTotalBalance = totalBalance;
                    if(getActivity() == null){
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvBalance.formatAndSetText(Utils.formatDecred(finalTotalBalance) + " DCR");
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void prepareHistoryData(){
        swipeRefreshLayout.setRefreshing(true);
        tempTxList.clear();
        transactionList.clear();
        loadTransactions();
        new Thread(){
            public void run(){
                try {
                    constants.wallet.getTransactions(OverviewFragment.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void saveTransactions(){
        try {
            File path = new File(getContext().getFilesDir()+"/savedata/");
            path.mkdirs();
            File file = new File(getContext().getFilesDir()+"/savedata/transactions");
            file.createNewFile();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(tempTxList);
            objectOutputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void loadTransactions(){
        try {
            File path = new File(getContext().getFilesDir()+"/savedata/");
            path.mkdirs();
            File file = new File(getContext().getFilesDir()+"/savedata/transactions");
            if(file.exists()){
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
                tempTxList = (List<Transaction>) objectInputStream.readObject();
                if(tempTxList.size() > 0) {
                    if (tempTxList.size() > 7) {
                        transactionList.addAll(tempTxList.subList(0, 7));
                    } else {
                        transactionList.addAll(tempTxList.subList(0, tempTxList.size() - 1));
                    }
                }
                transactionAdapter.notifyDataSetChanged();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onRefresh() {
        prepareHistoryData();
    }

    @Override
    public void onResult(String json) {
        TransactionsResponse response = TransactionsResponse.parse(json);
        if(getActivity() == null){
            return;
        }
        if(response.errorOccurred){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refresh.setVisibility(View.VISIBLE);
                    if(swipeRefreshLayout.isRefreshing()){
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    recyclerView.setVisibility(View.GONE);
                }
            });
        }
        else if(response.transactions.size() == 0){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refresh.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    if(swipeRefreshLayout.isRefreshing()){
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            });
        }else {
            final List<Transaction> temp = new ArrayList<>();
            for (int i = 0; i < response.transactions.size(); i++) {
                Transaction transaction = new Transaction();
                TransactionsResponse.TransactionItem item = response.transactions.get(i);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(item.timestamp * 1000);
                SimpleDateFormat sdf = new SimpleDateFormat(" dd yyyy, hh:mma", Locale.getDefault());
                transaction.setTxDate(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + sdf.format(calendar.getTime()).toLowerCase());
                transaction.setTime(item.timestamp);
                transaction.setTransactionFee(item.fee);
                transaction.setType(item.type);
                transaction.setHash(item.hash);
                transaction.setHeight(item.height);
                transaction.setDirection(item.direction);
                transaction.setAmount(item.amount);
                ArrayList<String> usedInput = new ArrayList<>();
                for (int j = 0; j < item.debits.size(); j++) {
                    transaction.totalInput += item.debits.get(j).previous_amount;
                    usedInput.add(item.debits.get(j).accountName + "\n" + Utils.formatDecred(item.debits.get(j).previous_amount));
                }
                ArrayList<String> output = new ArrayList<>();
                for (int j = 0; j < item.credits.size(); j++) {
                    transaction.totalOutput += item.credits.get(j).amount;
                    output.add(item.credits.get(j).address + "\n" + Utils.formatDecred(item.credits.get(j).amount));
                }
                transaction.setUsedInput(usedInput);
                transaction.setWalletOutput(output);
                temp.add(transaction);
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getBalance();
                    Collections.sort(temp, new TransactionSorter());
                    tempTxList.clear();
                    tempTxList.addAll(0, temp);
                    transactionList.clear();
                    if (tempTxList.size() > 0) {
                        if (tempTxList.size() > 7) {
                            transactionList.addAll(tempTxList.subList(0, 7));
                        } else {
                            transactionList.addAll(tempTxList.subList(0, tempTxList.size() - 1));
                        }
                        if (refresh.isShown()) {
                            refresh.setVisibility(View.INVISIBLE);
                        }
                    }
                    recyclerView.setVisibility(View.VISIBLE);
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    transactionAdapter.notifyDataSetChanged();
                    saveTransactions();
                }
            });
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == null){
                return;
            }
            if(intent.getAction().equals(Constants.ACTION_BLOCK_SCAN_COMPLETE)){
                prepareHistoryData();
            }else if(intent.getAction().equals(Constants.ACTION_NEW_TRANSACTION)){
                Transaction transaction = new Transaction();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(intent.getLongExtra(Constants.EXTRA_TRANSACTION_TIMESTAMP, 0) * 1000);
                SimpleDateFormat sdf = new SimpleDateFormat(" dd yyyy, hh:mma", Locale.getDefault());
                transaction.setTxDate(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + sdf.format(calendar.getTime()).toLowerCase());
                transaction.setTransactionFee(intent.getLongExtra(Constants.EXTRA_TRANSACTION_FEE, 0));
                transaction.setType(intent.getStringExtra(Constants.EXTRA_TRANSACTION_TYPE));
                transaction.setHash(intent.getStringExtra(Constants.EXTRA_TRANSACTION_HASH));
                transaction.setHeight(intent.getIntExtra(Constants.EXTRA_BLOCK_HEIGHT, 0));
                transaction.setAmount(intent.getLongExtra(Constants.EXTRA_AMOUNT, 0));
                transaction.setDirection(intent.getIntExtra(Constants.EXTRA_TRANSACTION_DIRECTION, -1));
                transaction.setUsedInput(intent.getStringArrayListExtra(Constants.EXTRA_TRANSACTION_INPUTS));
                transaction.setWalletOutput(intent.getStringArrayListExtra(Constants.EXTRA_TRANSACTION_OUTPUTS));
                transaction.setTotalInput(intent.getLongExtra(Constants.EXTRA_TRANSACTION_TOTAL_INPUT, 0));
                transaction.setTotalOutput(intent.getLongExtra(Constants.EXTRA_TRANSACTION_TOTAL_OUTPUT, 0));
                transaction.animate = true;
                transactionList.add(0, transaction);
                transactionAdapter.notifyDataSetChanged();
                if(transactionList.size() > 0){
                    transactionList.remove(transactionList.size() - 1);
                }
                getBalance();
            }else if(intent.getAction().equals(Constants.ACTION_TRANSACTION_CONFRIMED)){
                String hash = intent.getStringExtra(Constants.EXTRA_TRANSACTION_HASH);
                for(int i = 0; i < transactionList.size(); i++){
                    if(transactionList.get(i).getHash().equals(hash)){
                        Transaction transaction = transactionList.get(i);
                        transaction.setHeight(intent.getIntExtra(Constants.EXTRA_BLOCK_HEIGHT, -1));
                        transactionList.set(i, transaction);
                        transactionAdapter.notifyDataSetChanged();
                        break;
                    }
                }
                getBalance();
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("Overview OnPause");
        if(getActivity() != null){
            getActivity().unregisterReceiver(receiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("Overview OnResume");
        if(getActivity() != null) {
            IntentFilter filter = new IntentFilter(Constants.ACTION_BLOCK_SCAN_COMPLETE);
            filter.addAction(Constants.ACTION_NEW_TRANSACTION);
            filter.addAction(Constants.ACTION_TRANSACTION_CONFRIMED);
            getActivity().registerReceiver(receiver, filter);
        }
    }

    public void setMainA(){
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.displaySelectedScreen(R.id.nav_history);
    }
}
