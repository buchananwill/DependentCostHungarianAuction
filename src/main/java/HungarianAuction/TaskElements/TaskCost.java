package HungarianAuction.TaskElements;

public class TaskCost {
    private double sum = 0;
    private double product = 1.0;

    public TaskCost() {
    }

    public TaskCost(double sum) {
        this.sum = sum;
    }

    public double getSum() {
        return sum;
    }

    public void modifySum(double value) {
        this.sum += value;
    }

    public double getProduct() {
        return product;
    }

    public void modifyProduct(double value) {
        this.product *= value;
    }

    public double getFinalValue() {
        return sum * product;
    }


    public void resetSum() {
        sum = 0;
    }

    public void resetProduct() {
        product = 1;
    }
}
