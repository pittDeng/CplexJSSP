package com.learn;

import ilog.concert.*;
import ilog.cplex.*;

public class TestCplex {
    private int [][] assignM=null;
    private int [][] timeM=null;
    int numOfMachine;
    int numOfJob;
    int timeUpBound;
    int M;
    IloCplex cplex=null;
    IloNumVar [][] c_ik=null;
    int [][][] a_ihk=null;
    IloNumVar [][][] x_ijk=null;
    IloNumExpr obj=null;
    public void read(String filePath){
        ReadData rd=new ReadData();
        rd.read(filePath);
        assignM=rd.getAssignMatrix();
        timeM=rd.getTimeMatrix();
        numOfMachine=rd.getCols();
        numOfJob=rd.getRows();
        calculateTimeUpBound();
    }
    private void calculateTimeUpBound(){
        /*
        这里是计算所有东西的总加工时间，
        因为只要涉及优化，Makespan不可能比这个数值还要大。
         */
        this.timeUpBound=0;
        for(int i=0;i<timeM.length;++i){
            for(int j=0;j<timeM[i].length;++j){
                this.timeUpBound+=timeM[i][j];
            }
        }
        this.M=this.timeUpBound;
    }
    public void setModel(){
        try{
            cplex=new IloCplex();
            setVariables();
            setObjective();
            setLimitation();
            doOptimize();
        }catch (Exception e){

        }
    }
    private void doOptimize(){
        try{
        if(cplex.solve()) {
            System.out.println("Solution status=" + cplex.getStatus());
            System.out.println("Solution vlaue =" + cplex.getObjValue());
            for(int i=0;i<c_ik.length;++i){
                System.out.print(String.format("%d th",i));
                double [] val=cplex.getValues(c_ik[i]);
                for(int k=0;k<val.length;++k){
                    System.out.print(val[k]+"   ");
                }
                System.out.print("\n");
            }
        }
        cplex.end();}
        catch (IloException e){
            System.out.println("Optimization has encountered a problem");
            e.printStackTrace();
        }
    }
    private void setLimitation(){
        setOperationOrder();
        setMachineOrder();
    }
    private void setOperationOrder(){
        for(int i=0;i<a_ihk.length;++i){
            try{
                cplex.addGe(c_ik[i][assignM[i][0]],timeM[i][0]);
            }catch (IloException e){
                System.out.println("The first operation bounds setting has show some problem");
                e.printStackTrace();
            }

            for(int h=0;h<a_ihk[i].length;++h){
                for(int k=0;k<a_ihk[i].length;++k){
                    try{
                    cplex.addGe(cplex.sum(cplex.prod(1,c_ik[i][k]),cplex.prod(-1,c_ik[i][h])),timeM[i][findIndex(assignM[i],k)]-this.M*(1-a_ihk[i][h][k]));
                    }catch (IloException e){
                        System.out.println("OperationOrder setting has encounter some problem");
                        e.printStackTrace();
                    }
            }
        }
    }
    }
    private void setMachineOrder(){
        for(int i=0;i<x_ijk.length;++i){
            for(int j=0;j<x_ijk[i].length;++j){
                for(int k=0;k<x_ijk[i][j].length;++k){
                    if(i!=j){
                        try{
                            cplex.addGe(cplex.sum(c_ik[j][k],cplex.prod(-1,c_ik[i][k]),cplex.prod(M,cplex.sum(1,cplex.prod(-1,x_ijk[i][j][k])))),timeM[j][findIndex(assignM[j],k)]);
                            cplex.addLe(cplex.sum(x_ijk[i][j][k],x_ijk[j][i][k]),1);
                            cplex.addGe(cplex.sum(x_ijk[i][j][k],x_ijk[j][i][k]),1);
                        }catch (IloException e){
                            System.out.println("MachineOrder setting encounter some problem");
                            e.printStackTrace();
                        }
                        }
                }
            }
        }
    }
    private void setObjective(){
        if(c_ik[0].length<2){
            System.out.println("Problem size is less than 2");
            System.exit(-1);
        }
        try {
            obj = cplex.max(c_ik[0][0], c_ik[0][1]);
            for (int i = 0; i < c_ik.length; ++i) {
                for (int j = 0; j < c_ik[i].length; ++j) {
                    obj = cplex.max(obj, c_ik[i][j]);
                }
            }
            cplex.addMinimize(obj);
        }catch (IloException e){
            System.out.println("Objective setting has come across some exception");
            e.printStackTrace();
        }
    }
    private static int findIndex(int [] arr,int item){
        int index=0;
        for(;index<arr.length;++index){
            if(arr[index]==item)return index;
        }
        return -1;
    }
    private void setVariables() {
        /*
        这里可以初始化JSSP问题的所有变量
         */
        try{
            c_ik=new IloNumVar[numOfJob][numOfMachine];
            for(int i=0;i<numOfJob;++i){
                c_ik[i]=cplex.intVarArray(numOfMachine,0,timeUpBound);
            }
            a_ihk=new int[numOfJob][numOfMachine][numOfMachine];
            for(int i=0;i<assignM.length;++i){
                for(int h=0;h<assignM[i].length;++h){
                    for(int k=0;k<assignM[i].length;++k){
                        if(h<k){
                            a_ihk[i][assignM[i][h]][assignM[i][k]]=1;
                        }
                    }
                }
            }
//            for(int i=0;i<numOfJob;++i){
//                for(int h=0;h<numOfMachine;++h){
//                    a_ihk[i][h]=cplex.boolVarArray(numOfMachine);
//                }
//            }
            x_ijk=new IloNumVar[numOfJob][numOfJob][numOfMachine];
            for(int i=0;i<numOfJob;++i){
                for(int j=0;j<numOfJob;++j){
                    x_ijk[i][j]=cplex.boolVarArray(numOfMachine);
                }
            }
        }catch (IloException e){
            System.out.println("Error Message:");
            e.printStackTrace();
        }


    }
    public static void example(){
        try{
            IloCplex cplex=new IloCplex();
            int [] lb={1,1};
            int [] ub={Integer.MAX_VALUE,Integer.MAX_VALUE};
            //IloIntVar [] x=cplex.intVarArray(2,lb,ub);
            IloNumVar [] x=cplex.boolVarArray(2);
            IloNumVar [] y=cplex.intVarArray(2,lb,ub);
            int [] objvals={1,2};
            cplex.addMaximize(cplex.max(x[0],x[1]));
            //cplex.addMaximize(cplex.scalProd(x,objvals));
            cplex.addLe(cplex.sum(cplex.prod(4,x[0]),cplex.prod(1,x[1]),cplex.prod(1,y[0])),4);
            cplex.addLe(cplex.sum(cplex.prod(1,x[0]),cplex.prod(4,x[1]),cplex.prod(1,y[1])),4);
            if(cplex.solve()) {
                System.out.println("Solution status=" + cplex.getStatus());
                System.out.println("Solution vlaue =" + cplex.getObjValue());
                double[] val = cplex.getValues(x);
                double [] val1=cplex.getValues(y);
                for (int j = 0; j < val.length; ++j) {
                    System.out.println("Column" + j + "value=" + val[j]);
                }
                for (int j = 0; j < val.length; ++j) {
                    System.out.println("Column" + j + "value=" + val1[j]);
                }
            }
            cplex.end();
        }catch (IloException e){
            System.err.println("Concert Exception '"+e+"caught");
        }
    }
    public static void testJSSP(){
        long time1=System.currentTimeMillis();
        TestCplex testCplex=new TestCplex();
        testCplex.read("la32.txt");
        testCplex.setModel();
        System.out.println("\n\n\n"+(System.currentTimeMillis()-time1));
    }
    public static void main(String [] args){
        testJSSP();
    }
}
