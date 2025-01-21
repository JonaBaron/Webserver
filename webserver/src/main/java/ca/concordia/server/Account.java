package ca.concordia.server;

import java.util.concurrent.locks.ReentrantLock;

public class Account {
    //represent a bank account with a balance and withdraw and deposit methods
    private int balance;
    private int id;
    private final ReentrantLock lock = new ReentrantLock();

    public Account(int balance, int id){

        this.balance = balance;
        this.id = id;
    }
    
	public int getBalance(){
        return balance;
    }

    public void withdraw(int amount){
        balance -= amount;
    }

    public void deposit(int amount){
        balance += amount;
    }
    
    public int getId() {
        return id;
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
