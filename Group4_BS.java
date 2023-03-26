package mas2023.group4;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.NoModel;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;

/**
 * This is an abstract class used to implement a TimeDependentAgent Strategy
 * adapted from [1] [1] S. Shaheen Fatima Michael Wooldridge Nicholas R.
 * Jennings Optimal Negotiation Strategies for Agents with Incomplete
 * Information http://eprints.ecs.soton.ac.uk/6151/1/atal01.pdf
 * 
 * The default strategy was extended to enable the usage of opponent models.
 */
public class Group4_BS extends OfferingStrategy {

	/**
	 * k in [0, 1]. For k = 0.2 the agent starts with a bid
	 * with the utility of U_min + 0.8 * (U_max - U_min)
	 */
	private double k;
	/** Maximum target utility */
	private double Pmax;
	/** Minimum target utility */
	private double Pmin;
	/** Concession factor */
	private double e;
	/** The specified ratio factor to start the next stage of f(t) */
	private double a;
	/** Outcome space */
	private SortedOutcomeSpace outcomespace;

	/**
	 * Method which initializes the agent by setting all parameters. The
	 * parameter "e" and "a" required.
	 */
	@Override
	public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms,
			Map<String, Double> parameters) throws Exception {
		super.init(negoSession, parameters);
		if ((parameters.get("e") != null) && (parameters.get("a") != null)) {
			//初始化谈判session，获得outcome space和自己的utility space等信息
			this.negotiationSession = negoSession;
			//根据utility把outcome space里的出价排序
			outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
			//将有序化的outcome space载入谈判session
			negotiationSession.setOutcomeSpace(outcomespace);

			this.e = parameters.get("e");
			this.a = parameters.get("a");

			if (parameters.get("k") != null)
				this.k = parameters.get("k");
			else
				this.k = 0.2;

			if (parameters.get("min") != null)
				this.Pmin = parameters.get("min");
			else
				this.Pmin = negoSession.getMinBidinDomain().getMyUndiscountedUtil();

			if (parameters.get("max") != null) {
				Pmax = parameters.get("max");
			} else {
				BidDetails maxBid = negoSession.getMaxBidinDomain();
				Pmax = maxBid.getMyUndiscountedUtil();
			}

			this.opponentModel = model;
			
			this.omStrategy = oms;
		} else {
			throw new Exception("Constant \"e\" for the concession speed and " +
					"constant \"a\" for the turning point of f(t) should be set.");
		}
	}
	//need changes
	@Override
	public BidDetails determineOpeningBid() {
		return determineNextBid();
	}

	/**
	 * Simple offering strategy which retrieves the target utility and looks for
	 * the nearest bid if no opponent model is specified. If an opponent model
	 * is specified, then the agent return a bid according to the opponent model
	 * strategy.
	 */
	@Override
	public BidDetails determineNextBid() {

		double time = negotiationSession.getTime();
		double utilityGoal;
		utilityGoal = p(time);

		// System.out.println("[e=" + e + ", Pmin = " +
		// BilateralAgent.round2(Pmin) + "] t = " + BilateralAgent.round2(time)
		// + ". Aiming for " + utilityGoal);

		// if there is no opponent model available
		if (opponentModel instanceof NoModel) {
			nextBid = negotiationSession.getOutcomeSpace().getBidNearUtility(utilityGoal);
		} else {
			// 考虑根据对手的行为模式来选择不同的出价策略，greedy或者cooperative
			// 只考虑自己的utility target
			//if(opponentModel.isGreedy())
			nextBid = omStrategy.getBid(outcomespace, utilityGoal);
			//else if
			//
		}
		return nextBid;
	}

	/**
	 * From [1]:
	 * 
	 * A wide range of time dependent functions can be defined by varying the
	 * way in which f(t) is computed. However, functions must ensure that 0 <=
	 * f(t) <= 1, f(0) = k, and f(1) = 1.
	 * 
	 * That is, the offer will always be between the value range, at the
	 * beginning it will give the initial constant and when the deadline is
	 * reached, it will offer the reservation value.
	 * 
	 * For e = 0 (special case), it will behave as a Hardliner.
	 */
	public double f(double t) {
		if (e == 0)
			return k;
		double t_a = 0.0;
		double ft = k + (1 - k) * Math.pow(t, 1.0 / e);
		// 1-ft >= a, keep exponential f(t) unchanged
		if (ft <= 1-a)
			return ft;
		// if 1-ft < a, f(t) turns to a linear function
		else
			t_a = t;
			ft = a/(1-t_a) * t - a/(1-t_a) + 1;
			return ft;
	}

	/**
	 * Makes sure the target utility with in the acceptable range according to
	 * the domain. Goes from Pmax to Pmin!
	 * 
	 * @param t
	 * @return double
	 */
	public double p(double t) {
		return Pmin + (Pmax - Pmin) * (1 - f(t));
	}

	public NegotiationSession getNegotiationSession() {
		return negotiationSession;
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("e", 1.0, "Concession rate"));
		set.add(new BOAparameter("k", 0.0, "Offset"));
		set.add(new BOAparameter("min", 0.0, "Minimum utility"));
		set.add(new BOAparameter("max", 0.99, "Maximum utility"));
		set.add(new BOAparameter("a", 0.0, "turning point of f(t)"));
		// might need to change the default value a little bit

		return set;
	}

	@Override
	public String getName() {
		return "Group4_BS";
	}
}