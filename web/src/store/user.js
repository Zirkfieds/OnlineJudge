import $ from 'jquery'


export default{
    state:{
        id: "",
        username: "",
        photo: "",
        token: "",
        is_login: false,
        pulling_info: true,
        isChatOpen: false,
        friends: []
    },
    getters: {},
    mutations:{
        updateUser(state, user) {
            state.id = user.id;
            state.username = user.username;
            state.photo = user.photo;
            state.is_login = user.is_login;
        },
        updateToken(state, token) {
            state.token = token;
        },
        updatePullingInfo(state, pulling_info) {
            state.pulling_info = pulling_info;
        },
        logout(state) {
            state.id = "",
            state.username = "",
            state.photo = "",
            state.token = "",
            state.is_login = false
        },
        updateFriends(state){
            state.friends = JSON.parse(localStorage.getItem("friends"));
        },
        addFriend(state, friend){
            let newFriend = []
            let update = {}
            let f = false;
            for(let i = 0; i < state.friends.length; i++){
                if(state.friends[i].userKey != friend.userKey){
                    newFriend.push(state.friends[i])
                }else {
                    f = true;
                    update = state.friends[i];
                }
            }
            if(f)   newFriend.push(update)
            else    state.friends.push(friend)
            if(state.friends.length > 10) state.friends.slice(0, 1)
            localStorage.setItem("friends", state.friends)
        },
    },
    actions:{
        login(context, data) {
            $.ajax({
                url: "http://127.0.0.1:3000/user/account/token/",
                type: 'post',
                data: {
                    username: data.username,
                    password: data.password,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        localStorage.setItem("jwt_token", resp.token);
                        context.commit("updateToken", resp.token);
                        data.success(resp);
                    } else {
                        data.error(resp);
                    }
                },
                error(resp) {
                    data.error(resp);
                }
            });
        },
        getinfo(context, data) {
            $.ajax({
                url: "http://127.0.0.1:3000/user/account/info/",
                type: 'get',
                headers: {
                    Authorization: "Bearer " + context.state.token,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        context.commit("updateUser", {
                            ...resp,
                            is_login: true,
                        });

                        data.success(resp);
                    } else {
                        data.error(resp);
                    }
                },
                error(resp) {
                    data.error(resp);
                }
            });
        },
        getinfoInMainPage(context, data) {
            $.ajax({
                url: "http://127.0.0.1:3000/index/info/",
                type: 'post',
                headers: {
                    Authorization: "Bearer " + context.state.token,
                },
                success() {
                    data.success();
                },
                error(resp) {
                    console.log(resp);
                }
            });
        },
        logout(context) {
            localStorage.removeItem("jwt_token");
            context.commit("logout");
        },
        sendSubmission(context, data){
            $.ajax({
                url: "http://127.0.0.1:3000/user/submission/offersub/",
                type: 'POST',
                headers: {
                    Authorization: "Bearer " + context.state.token,
                },
                data: {
                    userKey: data.userKey,
                    code: data.content,
                    language: data.language,
                    debugInfo: data.debugInfo,
                    targetProblem: data.targetProblem,
                    SUUID: data.SUUID,
                },
                success(resp) {
                    data.success(resp)
                },
                error() {
                    console.log(this.data)
                }
            });
        },
        sendPollRequest(context, data){
            $.ajax({
                url: "http://127.0.0.1:3000/user/submission/pollret/",
                type: 'POST',
                headers: {
                    Authorization: "Bearer " + context.state.token,
                },
                data: {
                    SUUID: data.SUUID,
                },
                success(resp) {
                    data.success(resp)
                },
                error() {
                    console.log(this.data)
                }
            });
        },
    },
    modules:{}
}