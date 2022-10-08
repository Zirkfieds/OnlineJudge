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

    private final int entriesPerPage = 2;

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
    public JSONObject getProblemListOverview(Integer page) {

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
        ret.put("problemList", problemList);
        return ret;
    }

    @Override
    public JSONObject getProblemListByKey(String key, Integer page) {

        IPage<Problem> problemIPage = new Page<>(page, entriesPerPage);

        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("problemkey");
        queryWrapper.eq("problemkey", Integer.parseInt(key));
        List<Problem> problems = problemMapper.selectPage(problemIPage, queryWrapper).getRecords();

        JSONObject ret = new JSONObject();
        ArrayList<JSONObject> problemList = new ArrayList<>();

        for (Problem p : problems) {
            problemList.add(problemInfoExtractor(p));
        }

        ret.put("problemCount", problems.size());
        ret.put("problemList", problemList);
        return ret;
    }

    @Override
    public JSONObject getProblemListByName(String name, Integer page) {
        IPage<Problem> problemIPage = new Page<>(page, entriesPerPage);

        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("problemkey");
        queryWrapper.like("problemname", name);
        List<Problem> problems = problemMapper.selectPage(problemIPage, queryWrapper).getRecords();

        JSONObject ret = new JSONObject();
        ArrayList<JSONObject> problemList = new ArrayList<>();

        for (Problem p : problems) {
            problemList.add(problemInfoExtractor(p));
        }

        ret.put("problemCount", problems.size());
        ret.put("problemList", problemList);
        return ret;
    }

    private Set<Problem> getIntersectionSet(Set<Problem> A, Set<Problem> B) {

        Set<Problem> ret = new HashSet<>(A);
        ret.retainAll(B);
        return ret;
    }

    @Override
    public JSONObject getProblemListByTags(ArrayList<String> tags, boolean isUnion, Integer page) {

        IPage<Problem> problemIPage = new Page<>(page, entriesPerPage);

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

}
