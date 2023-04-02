package mas2023.group4;

import java.util.*;
import java.util.Map.Entry;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

/**
 * This is an improved implementation based on the Opponent Model of
 * HardHeaded Frequency Model in the boa framework.
 *
 * The original default: learning coef l = 0.2; learnValueAddition v = 1.0.
 *
 * New default: roundToUpdate = 4; numberOfRounds = 3.
 *
 * paper: https://ii.tudelft.nl/sites/default/files/boa.pdf
 */
public class Group4_OM extends OpponentModel {

	/*
	 * the learning coefficient is the weight that is added each turn to the
	 * issue weights which changed. It's a trade-off between concession speed
	 * and accuracy.
	 */
	private double learnCoef;
	/*
	 * value which is added to a value if it is found. Determines how fast the
	 * value weights converge.
	 */
	private int learnValueAddition;
	private int amountOfIssues;
	private double goldenValue;
	private double roundToUpdate;
	/*
	 * roundToUpdate is to consider the opponents' offers from previous rounds
	 * starting from a certain round.
	 */
	private double numberOfRounds;
	// numberOfRounds is the number of previous rounds to consider.

	@Override
	public void init(NegotiationSession negotiationSession,
					 Map<String, Double> parameters) {
		this.negotiationSession = negotiationSession;
		if (parameters != null && parameters.get("l") != null) {
			learnCoef = parameters.get("l");
		} else {
			learnCoef = 0.2;
		}
		if (parameters.get("roundToUpdate") != null) {
			roundToUpdate = parameters.get("roundToUpdate").intValue();
		} else {
			roundToUpdate = 4;
		}
		/**
		 * If the parameters contain roundToUpdate, the integer part of
		 * its corresponding value is assigned to roundToUpdate.
		 * Otherwise, 4 is assigned to roundToUpdate, which means that the
		 * opponent's previous bid history will be considered from the fourth
		 * round onwards.
		 */
		if (parameters.get("numberOfRounds") != null) {
			numberOfRounds = parameters.get("numberOfRounds").intValue();
		} else {
			numberOfRounds = 3;
		}
		/**
		 * If the parameters contain numberOfRounds, the integer part of
		 * its corresponding value is assigned to roundToUpdate.
		 * Otherwise, 3 is assigned to numberOfRounds, which means that each my bid
		 * will consider the previous three rounds of the opponent's bid history.
		 */
		learnValueAddition = 1;
		opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession
				.getUtilitySpace().copy();
		amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
		/*
		 * This is the value to be added to weights of unchanged issues before
		 * normalization. Also the value that is taken as the minimum possible
		 * weight, (therefore defining the maximum possible also).
		 */
		goldenValue = learnCoef / amountOfIssues;

		initializeModel();

	}

	@Override
	public void updateModel(Bid opponentBid, double time) {
		int historySize = negotiationSession.getOpponentBidHistory().size();
		//Assign the size of OpponentBidHistory to historySize

		if (historySize < roundToUpdate) {
			return;
		}
		/*
		 * If historySize < roundToUpdate, then return directly,
		 * without considering the opponent's bid history.
		 */

		int numberOfUnchanged = 0;
		ArrayList<HashMap<Integer, Integer>> diffs = new ArrayList<>();

		for (int i = 1; i <= numberOfRounds - 1; i++) {

			int previousRoundIndex = historySize - 1 ;
			//  previousRoundIndex is the opponent's last offer round
			int prevPreviousRoundIndex = historySize - 1 - i;
			// prePreviousRoundIndex is the opponent's previous last offer round

			if (prevPreviousRoundIndex <= 0) {
				break;
			}
			/*
			 * Cycle through every two similar rounds and ensure that round 0
			 * and negative rounds are not indexed.
			 */

			BidDetails previousOppBid = negotiationSession.getOpponentBidHistory()
					.getHistory()
					.get(previousRoundIndex);
			/*
			 * Get the previous round's bid from the opponent's bid history
			 * and assign it to previousOppBid.
			 */
			BidDetails prevPreviousOppBid = negotiationSession.getOpponentBidHistory()
					.getHistory()
					.get(prevPreviousRoundIndex);
			/*
			 * Get the previous previous round's bid from the opponent's bid history
			 * and assign it to prevPreviousOppBid.
			 */
			HashMap<Integer, Integer> lastDiffSet = determineDifference(prevPreviousOppBid, previousOppBid);
			diffs.add(lastDiffSet);
			/*
			 * Call the determineDifference method to calculate the difference between
			 * the previous previous round and the previous bid and assign it to lastDiffSet.
			 */
		}

		for (HashMap<Integer, Integer> diff : diffs) {
			for (Integer i : diff.keySet()) {
				if (diff.get(i) == 0) {
					numberOfUnchanged++;
				}
			}
		}
		/*
		 * loop comparison, if diff is 0, it means no change in the previous two offers,
		 * so numberOfUnchanged + 1
		 */

		double totalSum = 1D + goldenValue * numberOfUnchanged;
		for (HashMap<Integer, Integer> diff : diffs) {
			for (Entry<Integer, Integer> e : diff.entrySet()) {
				try {
					int issueNum = e.getKey();
					double weight = opponentUtilitySpace.getWeight(issueNum);
					double newWeight = (weight + goldenValue * e.getValue())
							/ totalSum;
					Objective issue = opponentUtilitySpace.getDomain()
							.getObjectivesRoot().getObjective(issueNum);
					opponentUtilitySpace.setWeight(issue, newWeight);
					opponentUtilitySpace.normalizeWeights();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		try {
			for (Issue issue : opponentUtilitySpace.getDomain().getIssues()) {
				int issueNumber = issue.getNumber();
				IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
				EvaluatorDiscrete evaluator = (EvaluatorDiscrete) opponentUtilitySpace
						.getEvaluator(issueNumber);

				for (ValueDiscrete value : issueDiscrete.getValues()) {
					int lastNumberOfRealBidsWithValue = 0;
					for (int i = 0; i < numberOfRounds - 1; i++) {
						BidDetails oppBid = negotiationSession.getOpponentBidHistory()
								.getHistory()
								.get(negotiationSession.getOpponentBidHistory().size() - 1 - i);
						if (oppBid.getBid().getValue(issueNumber).equals(value)) {
							lastNumberOfRealBidsWithValue++;
						}
					}

					int newEvaluation = (int) (evaluator.getEvaluation(value)
							+ learnValueAddition * lastNumberOfRealBidsWithValue);
					evaluator.setEvaluation(value, newEvaluation);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}


	@Override
	public double getBidEvaluation(Bid bid) {
		double result = 0;
		try {
			result = opponentUtilitySpace.getUtility(bid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getName() {
		return "Group4_OM";
	}

	@Override

	/**
	 * If roundToUpdate = 3.0 and numberOfRounds = 2.0, the Opponent Model of our group
	 * will equal to that of HardHeaded Frequency Model.
	 * But the advantage of our Opponent Model is that it can be adapted at any time.
	 */
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("l", 0.2,
				"The learning coefficient determines how quickly the issue weights are learned"));
		set.add(new BOAparameter("roundToUpdate", 4.0,
				"Round number to start updating based on a number of rounds"));
		set.add(new BOAparameter("numberOfRounds", 3.0,
				"Number of rounds to update based on"));
		return set;
	}

	/**
	 * Init to flat weight and flat evaluation distribution
	 */
	private void initializeModel() {
		double commonWeight = 1D / amountOfIssues;

		for (Entry<Objective, Evaluator> e : opponentUtilitySpace
				.getEvaluators()) {

			opponentUtilitySpace.unlock(e.getKey());
			e.getValue().setWeight(commonWeight);
			try {
				// set all value weights to one (they are normalized when
				// calculating the utility)
				for (ValueDiscrete vd : ((IssueDiscrete) e.getKey())
						.getValues())
					((EvaluatorDiscrete) e.getValue()).setEvaluation(vd, 1);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Determines the difference between bids. For each issue, it is determined
	 * if the value changed. If this is the case, a 1 is stored in a hashmap for
	 * that issue, else a 0.
	 *
	 //	 * @param a
	 *            bid of the opponent
	 //	 * @param another
	 *            bid
	 * @return
	 */
	private HashMap<Integer, Integer> determineDifference(BidDetails first,
														  BidDetails second) {

		HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
		try {
			for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
				Value value1 = first.getBid().getValue(i.getNumber());
				Value value2 = second.getBid().getValue(i.getNumber());
				diff.put(i.getNumber(), (value1.equals(value2)) ? 0 : 1);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return diff;
	}

}
