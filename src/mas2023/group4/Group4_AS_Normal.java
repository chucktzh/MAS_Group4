package mas2023.group4;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;

/**
 * This Acceptance Condition will accept an opponent bid if the utility is
 * higher than the bid the agent is ready to present.
 *
 * Decoupling Negotiating Agents to Explore the Space of Negotiation Strategies
 * T. Baarslag, K. Hindriks, M. Hendrikx, A. Dirkzwager, C.M. Jonker
 *
 */
public class Group4_AS_Normal extends AcceptanceStrategy {
	/**
	 * parameters used when compare opponent's offer and mine
	 */
	private double a;
	private double b;
	/**
	 * Record the round when we decide wait and see
	 * if the cooperating opponent will offer us better bid.
	 */
	private int t_WaitAndSee;
	/**
	 * Time factor T, to determine when to totally surrender
	 */
	double T = 1.0; // Never surrender by default

	/**
	 * Empty constructor for the BOA framework.
	 */
	public Group4_AS_Normal() {
	}

	public Group4_AS_Normal(NegotiationSession negoSession, OfferingStrategy strat,
							double alpha, double beta) {
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;
		this.a = alpha;
		this.b = beta;
		this.t_WaitAndSee = 0;
	}

	@Override
	public void init(NegotiationSession negoSession, OfferingStrategy strat,
			OpponentModel opponentModel, Map<String, Double> parameters)
			throws Exception {
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;

		if (parameters.get("T") != null){
			T = parameters.get("T");
		}else{
			System.out.println("Time factor T is not set, T = 1.0 by default");
		}
		if (parameters.get("a") != null || parameters.get("b") != null) {
			a = parameters.get("a");
			b = parameters.get("b");
		} else {
			a = 1;
			b = 0;
		}
	}

	@Override
	public String printParameters() {
		String str = "[a: " + a + " b: " + b + "]";
		return str;
	}

	@Override
	public Actions determineAcceptability() {
		double nextMyBidUtil = offeringStrategy.getNextBid()
				.getMyUndiscountedUtil();
		double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory()
				.getLastBidDetails().getMyUndiscountedUtil();
		// Calculate the utility difference between the opponent's offer and the agent's next offer
		double utilityDifference = nextMyBidUtil - (a * lastOpponentBidUtil + b);

		// Calculate the minimal acceptable utility i.e. the target utility for now
		double time = negotiationSession.getTime();
		double myUtilThreshold = 0.8;
		if (offeringStrategy instanceof Group4_BS){
			Group4_BS BS = (Group4_BS) offeringStrategy;
			myUtilThreshold = BS.p(time);
		}else {
			throw new ClassCastException("The offeringStrategy class passed to AS " +
					"can't be downcast to \"Group4_BS\".");
		}

		int numberOfRounds = negotiationSession.getOpponentBidHistory().size();

		//judge opponent is greedy(1) or cooperative(0)
		boolean isOpponentGreedy = true; // opponentModel.isGreedy();

		// Time is running out. Accept anyway.
		if (time > T){
			return Actions.Accept;
		}
		// Accept the offer if the opponent's utility is higher than my next offer
		if (utilityDifference <= 0) {
			return Actions.Accept;
		} else if(isOpponentGreedy && lastOpponentBidUtil >= myUtilThreshold){
			// Accept when greedy opponent bids with utility above my threshold
			return Actions.Accept;
		}
//		else if (numberOfRounds == t_WaitAndSee + 3){
//			t_WaitAndSee = 0;
//			return Actions.Accept;
//		} else if(!isOpponentGreedy && lastOpponentBidUtil >= myUtilThreshold){
//			t_WaitAndSee = numberOfRounds;
//		}
		else{
			// Reject the offer otherwise
			return Actions.Reject;
		}

	}


	@Override
	public Set<BOAparameter> getParameterSpec() {

		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("a", 1.0,
				"Accept when the opponent's utility * a + b is greater than the utility of our current bid"));
		set.add(new BOAparameter("b", 0.0,
				"Accept when the opponent's utility * a + b is greater than the utility of our current bid"));
		set.add(new BOAparameter("T", 1.0,
				"Accept when time > T."));
		return set;
	}

	@Override
	public String getName() {
		return "Group4_AS_Normal";
	}
}