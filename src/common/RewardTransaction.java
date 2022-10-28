package common;

import java.util.Objects;

public class RewardTransaction extends Transaction {

    // post relative to which the transaction was made
    private int postRewardedId;
    
    public RewardTransaction(double value, int postRewardedId) {
        super(value);
        this.postRewardedId = Objects.requireNonNull(postRewardedId, "You have to provide the ID of the post rewarded");
    }


    
    /** 
     * @return int
     */
    public int getPostRewardedId() {
        return this.postRewardedId;
    }


    
    /** 
     * @return String
     */
    @Override
    public String toString() {
        return "[Value: "+this.getValue()+", PostID: "+this.postRewardedId+", Time: "+this.getTime()+']';
    }
}
