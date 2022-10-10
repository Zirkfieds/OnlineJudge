package com.oj.onlinejudge.service.impl.problems;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.onlinejudge.mapper.ProblemMapper;
import com.oj.onlinejudge.mapper.SubmissionMapper;
import com.oj.onlinejudge.pojo.Problem;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.oj.onlinejudge.pojo.Submission;
import com.oj.onlinejudge.service.problems.ProblemListService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProblemListServiceImpl implements ProblemListService {

    @Autowired
    private ProblemMapper problemMapper;
    @Autowired
    private SubmissionMapper submissionMapper;
    private final int entriesPerPage = 10;

    public JSONObject problemInfoExtractor(Problem p) {

        JSONObject problem = new JSONObject();
        problem.put("problemKey", p.getProblemkey());
        problem.put("problemName", p.getProblemname());
        problem.put("problemSource", p.getSource());

        double acAttempts = 0, totAttempts = 0;
        QueryWrapper<Submission> attemptWrapper = new QueryWrapper<>();
        attemptWrapper.eq("problemkey", Integer.toString(p.getProblemkey()));
        List<Submission> attemptEntries = submissionMapper.selectList(attemptWrapper);

        totAttempts = attemptEntries.size();
        if (totAttempts != 0) {
            for (Submission s : attemptEntries) {
                if (s.getResult().equals("Accepted")) {
                    acAttempts++;
                }
            }
            problem.put("AcceptedPct", (int) (100 * (acAttempts) / (totAttempts)));
        } else {
            problem.put("AcceptedPct", 0);
        }

        String tagStr = p.getTag();
        String[] tags = tagStr.split(" ");
        problem.put("problemTags", tags);

        return problem;
    }

    @Override
    public JSONObject masterProblemListGetter(String problemKey,
                                              String userKey,
                                              String problemName,
                                              ArrayList<String> tags,
                                              boolean isUnion,
                                              Integer state,
                                              Integer page) {

        boolean enableKeyFilter = !problemKey.isEmpty();
        boolean enableNameFilter = !problemName.isEmpty();
        boolean enableTagFilter = !tags.isEmpty();
        if (userKey.isEmpty()) {
            state = 0;
        }
        boolean enableStateFilter = state != 0;

        JSONObject ret = new JSONObject();

        if (!enableKeyFilter && !enableNameFilter && !enableTagFilter && !enableStateFilter) {
            return problemListGetter(page);
        }
        ArrayList<JSONObject> problemList = new ArrayList<>();

        IPage<Problem> problemIPage = new Page<>(page, entriesPerPage);
        QueryWrapper<Problem> masterWrapper = new QueryWrapper<>();

        Set<Integer> base = new HashSet<>();
        if (enableKeyFilter) {
            base = getProblemListByKey(problemKey, page);
        }
        if (enableNameFilter) {
            if (base.isEmpty()) {
                base = getProblemListByName(problemKey, page);
            }
            else {
                base.retainAll(getProblemListByName(problemName, page));
            }
        }
        if (enableTagFilter) {
            if (base.isEmpty()) {
                base = getProblemListByTags(tags, isUnion, page);
            }
            else {
                base.retainAll(getProblemListByTags(tags, isUnion, page));
            }
        }
        if (enableStateFilter) {
            if (base.isEmpty()) {
                base = getProblemListByState(userKey, state, page);
            }
            else {
                base.retainAll(getProblemListByState(userKey, state, page));
            }
        }

        for (int key : base) {
            masterWrapper.eq("problemkey", key).or();
        }
        List<Problem> problems = problemMapper.selectPage(problemIPage, masterWrapper).getRecords();

        for (Problem p : problems) {
            problemList.add(problemInfoExtractor(p));
        }

        ret.put("problemCount", problems.size());
        ret.put("totalPages",problemMapper.selectCount(masterWrapper));
        ret.put("perPage", entriesPerPage);
        ret.put("problemList", problemList);
        return ret;
    }

    @Override
    public JSONObject problemListGetter(Integer page) {

        IPage<Problem> problemIPage = new Page<>(page, entriesPerPage);

        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("problemKey");
        List<Problem> problems = problemMapper.selectPage(problemIPage, queryWrapper).getRecords();

        JSONObject ret = new JSONObject();
        ArrayList<JSONObject> problemList = new ArrayList<>();

        for (Problem p : problems) {
            problemList.add(problemInfoExtractor(p));
        }

        ret.put("problemCount", problems.size());
        ret.put("totalPages", problemMapper.selectCount(null));
        ret.put("perPage", entriesPerPage);
        ret.put("problemList", problemList);
        return ret;
    }

    @Override
    public Set<Integer> getProblemListByKey(String problemKey, Integer page) {

        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("problemkey");
        queryWrapper.like("problemkey", Integer.parseInt(problemKey));
        List<Problem> problems = problemMapper.selectList(queryWrapper);

        Set<Integer> ret = new HashSet<>();
        for (Problem p : problems) {
            ret.add(p.getProblemkey());
        }
        return ret;
    }

    @Override
    public Set<Integer> getProblemListByName(String name, Integer page) {

        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("problemname", name);
        queryWrapper.orderByAsc("problemkey");
        List<Problem> problems = problemMapper.selectList(queryWrapper);

        Set<Integer> ret = new HashSet<>();
        for (Problem p : problems) {
            ret.add(p.getProblemkey());
        }
        return ret;
    }

    private Set<Problem> getIntersectionSet(Set<Problem> A, Set<Problem> B) {

        Set<Problem> ret = new HashSet<>(A);
        ret.retainAll(B);
        return ret;
    }

    @Override
    public Set<Integer> getProblemListByTags(ArrayList<String> tags, Boolean isUnion, Integer page) {

        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();

        if (isUnion) {
            for (String tag : tags) {
                queryWrapper.like("tag", tag).or();
            }
        } else {
            for (String tag : tags) {
                queryWrapper.like("tag", tag);
            }
        }
        queryWrapper.orderByAsc("problemkey");
        List<Problem> problems = problemMapper.selectList(queryWrapper);

        Set<Integer> ret = new HashSet<>();
        for (Problem p : problems) {
            ret.add(p.getProblemkey());
        }
        return ret;
    }

    @Override
    public Set<Integer> getProblemListByState(String userKey, Integer state, Integer page) {

        // state: 0 -> bypass state filter, 1 -> AC entries only, 2 -> entries whose results are not AC

        QueryWrapper<Submission> subQueryWrapper = new QueryWrapper<>();
        subQueryWrapper.orderByAsc("problemkey");
        subQueryWrapper.eq("userkey", Integer.parseInt(userKey));
        List<Submission> submissions = submissionMapper.selectList(subQueryWrapper);

        Set<Integer> acceptedKeys = new HashSet<>();
        Set<Integer> attemptedKeys = new HashSet<>();

        for (Submission s : submissions) {
            if ("Accepted".equals(s.getResult())) {
                acceptedKeys.add(s.getProblemkey());
            } else {
                attemptedKeys.add(s.getProblemkey());
            }
        }

        attemptedKeys.removeAll(acceptedKeys);
        return state == 1 ? acceptedKeys : attemptedKeys;
    }

}