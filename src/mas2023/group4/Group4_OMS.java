package mas2023.group4;

import java.util.List;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;

//Our agent’s opponent model strategic plan to based on a modification of the "Best
//		bid". We plan and consider setting up a certain threshold
//		which is the reservation bid of the agent’s own bid and the
//		opponent’s bid at the beginning in other components to get
//		the best payoff in case overpaying or giving up too much
//		appeared as the outcome
//我们计划并考虑在其他组成部分中设置一个阈值，即代理人自己的出价和对手在开始时的出价的保留出价，以在出现多付或放弃过多的情况下获得最佳回报


/*对手模型策略与Genius安装文件夹中的默认BestBid.java相比，没有任何改变。
		对手模型策略（OMS）保留了初始OMS中已经存在的每一个功能，以确保其组件的功能与其他BOA组件一样，即它们可以根据自己的需要与其他BOA组件混合和匹配。
		由于时间的限制，我们几乎没有对OMS和OMS中的方法进行实验。在win.win的当前状态下，OMS对于任何组件的正常运行都是不必要的。
		然而，在某些情况下，利用OMS可以对谈判作出很大的改进。*/

/**
 * This class uses an opponent model to determine the next bid for the opponent,
 * while taking the opponent's preferences into account. The opponent model is
 * used to select the best bid.
 * 
 */
public class Group4_OMS extends OMStrategy {



	/**
	 * weightAgentUtility: parameter that balances the agent's utility and the opponent's utility
	 * opponentReservationValue: parameter that record the lower threshold for the opponent's utility
	 */
	double updateThreshold = 1.1;
	double weightAgentUtility = 0.5;
	double opponentReservationValue = 0.0;



	/**
	 * Initializes the opponent model strategy. If a value for the parameter t
	 * is given, then it is set to this value. Otherwise, the default value is
	 * used.
	 *
	 * @param negotiationSession
	 *            state of the negotiation.
	 * @param model
	 *            opponent model used in conjunction with this opponent modeling
	 *            strategy.
	 * @param parameters
	 *            set of parameters for this opponent model strategy.
	 */

	@Override
	public void init(NegotiationSession negotiationSession, OpponentModel model, Map<String, Double> parameters) {
		super.init(negotiationSession, model, parameters);
		if (parameters.get("t") != null) {
			updateThreshold = parameters.get("t").doubleValue();
		} else {
//			System.out.println("OMStrategy assumed t = 1.1");
		}

		if (parameters.get("r") != null) {
			opponentReservationValue = parameters.get("r").doubleValue();
		} else {
//			System.out.println("OMStrategy assumed r = 0.0");
		}

		if (parameters.get("w") != null) {
			weightAgentUtility = parameters.get("w").doubleValue();
		} else {
//			System.out.println("OMStrategy assumed w = 0.5");
		}
	}






	/**
	 * We take both our own utility and the opponent's utility into consideration (as we mentioned in the assignment1)
	 * @param allBids
	 *            set of similarly preferred bids
//	 * @param list
	 *            list of the bids considered for offering.
	 * @return bid to be offered to opponent.
	 */
	@Override
	public BidDetails getBid(List<BidDetails> allBids) {

		// If there is only a single bid, return this bid
		if (allBids.size() == 1) {
			return allBids.get(0);
		}



		// Check that not all bids are assigned at utility of 0
		boolean allWereZero = true;

		// Record the best of the calculated weight score of the bid during the decision process
		double bestScore = -1;
		BidDetails bestBid = allBids.get(0);


		for (BidDetails bid : allBids) {
			double opponentEvaluation = model.getBidEvaluation(bid.getBid());

			// Get our own agent's utility
			double agentOwnUtility = negotiationSession.getUtilitySpace().getUtility(bid.getBid());

			// Based on the weight of the utility between our own agent and the opponent to calculate the score
			double score = weightAgentUtility * agentOwnUtility + (1 - weightAgentUtility) * opponentEvaluation;

			// If the evaluation value even not achieve the reservation value, the just not accept the new bid.
			if (opponentEvaluation > opponentReservationValue) {
				allWereZero = false;
			}

			// If not, and if the score also gets better, than accept the new bid as a better bid and record the score in the bestScore.
			if (score > bestScore && opponentEvaluation > opponentReservationValue) {
				bestBid = bid;
				bestScore = score;
			}
		}

		// When parameter allWereZero is true which means that opponent model does not provide useful information
		// Instead of randomising the return bid, we search through our own utilityspace to maximise our own utility
		if (allWereZero) {
			bestBid = null;
			double maxAgentUtility = -1;
			for (BidDetails bid : allBids) {
				double agentOwnUtility = negotiationSession.getUtilitySpace().getUtility(bid.getBid());
				if (agentOwnUtility > maxAgentUtility) {
					maxAgentUtility = agentOwnUtility;
					bestBid = bid;
				}
			}
			return bestBid;
		}

		return bestBid;
	}



	/**
	 * The opponent model may be updated, unless the time is higher than a given constant.
	 *
	 * @return true if model may be updated.
	 */
	@Override
	public boolean canUpdateOM() {
		return negotiationSession.getTime() < updateThreshold;
	}



	/**
	 * Returns a set of BOA parameters for our opponent model strategy.
	 * compared with the example, we add two new parameter (we have explained them in the beginning of this file)
	 * @return Returns this set.
	 */
	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("t", 1.1, "Time after which the OM should not be updated"));
		set.add(new BOAparameter("w", 0.5, "Weight given to the agent's utility"));
		set.add(new BOAparameter("r", 0.0, "Reservation value for the opponent's bid"));
		return set;
	}





	/**
	 * Returns the name of our opponent model strategy.
	 * @return return the name
	 */
	@Override
	public String getName() {
		return "Group4_OMS";
	}
}