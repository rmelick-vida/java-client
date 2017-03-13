package io.split.engine.experiments;

import com.google.common.collect.Lists;
import io.split.client.dtos.*;
import io.split.client.dtos.Matcher;
import io.split.engine.matchers.*;
import io.split.engine.segments.Segment;
import io.split.engine.segments.SegmentFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Converts io.codigo.dtos.Experiment to io.codigo.engine.splits.ParsedExperiment.
 *
 * @author adil
 */
public final class SplitParser {

    public static final int CONDITIONS_UPPER_LIMIT = 50;
    private static final Logger _log = LoggerFactory.getLogger(SplitParser.class);

    private SegmentFetcher _segmentFetcher;

    public SplitParser(SegmentFetcher segmentFetcher) {
        _segmentFetcher = segmentFetcher;
        checkNotNull(_segmentFetcher);
    }

    public ParsedSplit parse(Split split) {
        try {
            return parseWithoutExceptionHandling(split);
        } catch (Throwable t) {
            _log.error("Could not parse split: " + split, t);
            return null;
        }
    }

    private ParsedSplit parseWithoutExceptionHandling(Split split) {
        if (split.status != Status.ACTIVE) {
            return null;
        }

        if (split.conditions.size() > CONDITIONS_UPPER_LIMIT) {
            _log.warn(String.format("Dropping Split name=%s due to large number of conditions(%d)",
                    split.name, split.conditions.size()));
            return null;
        }

        List<ParsedCondition> parsedConditionList = Lists.newArrayList();

        for (Condition condition : split.conditions) {
            List<Partition> partitions = condition.partitions;
            CombiningMatcher matcher = toMatcher(condition.matcherGroup);
            parsedConditionList.add(new ParsedCondition(matcher, partitions, condition.label));
        }

        return new ParsedSplit(split.name, split.seed, split.killed, split.defaultTreatment, parsedConditionList, split.trafficTypeName, split.changeNumber, split.algo);
    }

    private CombiningMatcher toMatcher(MatcherGroup matcherGroup) {
        List<io.split.client.dtos.Matcher> matchers = matcherGroup.matchers;
        checkArgument(!matchers.isEmpty());

        List<AttributeMatcher> toCombine = Lists.newArrayList();

        for (io.split.client.dtos.Matcher matcher : matchers) {
            toCombine.add(toMatcher(matcher));
        }

        return new CombiningMatcher(matcherGroup.combiner, toCombine);
    }


    private AttributeMatcher toMatcher(Matcher matcher) {
        io.split.engine.matchers.Matcher delegate = null;
        switch (matcher.matcherType) {
            case ALL_KEYS:
                delegate = new AllKeysMatcher();
                break;
            case IN_SEGMENT:
                checkNotNull(matcher.userDefinedSegmentMatcherData);
                Segment segment = _segmentFetcher.segment(matcher.userDefinedSegmentMatcherData.segmentName);
                delegate = new UserDefinedSegmentMatcher(segment);
                break;
            case WHITELIST:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new WhitelistMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case EQUAL_TO:
                checkNotNull(matcher.unaryNumericMatcherData);
                delegate = new EqualToMatcher(matcher.unaryNumericMatcherData.value, matcher.unaryNumericMatcherData.dataType);
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                checkNotNull(matcher.unaryNumericMatcherData);
                delegate = new GreaterThanOrEqualToMatcher(matcher.unaryNumericMatcherData.value, matcher.unaryNumericMatcherData.dataType);
                break;
            case LESS_THAN_OR_EQUAL_TO:
                checkNotNull(matcher.unaryNumericMatcherData);
                delegate = new LessThanOrEqualToMatcher(matcher.unaryNumericMatcherData.value, matcher.unaryNumericMatcherData.dataType);
                break;
            case BETWEEN:
                checkNotNull(matcher.betweenMatcherData);
                delegate = new BetweenMatcher(matcher.betweenMatcherData.start, matcher.betweenMatcherData.end, matcher.betweenMatcherData.dataType);
                break;
            default:
                throw new IllegalArgumentException("Unknown matcher type: " + matcher.matcherType);
        }

        checkNotNull(delegate, "We were not able to create a matcher for: " + matcher.matcherType);

        String attribute = null;
        if (matcher.keySelector != null && matcher.keySelector.attribute != null) {
            attribute = matcher.keySelector.attribute;
        }

        boolean negate = matcher.negate;


        return new AttributeMatcher(attribute, delegate, negate);
    }


}
