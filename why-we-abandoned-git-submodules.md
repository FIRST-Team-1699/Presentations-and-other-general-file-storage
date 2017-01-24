
[Source](http://somethingsinistral.net/blog/git-submodules-are-probably-not-the-answer/ "Permalink to Git submodules are probably not the answer")

# Git submodules are probably not the answer

**2013-05-31 Update**: Using Puppet? Check out [R10K][1]!

As stated in a [random blog post][2], git and puppet can be the best of friends and make your life a lot easier. However, when using git and puppet modules, you'll run into a couple of cases where using git and puppet starts becoming harder.

### Case 1: Using external modules

Puppet modules, when properly written, are extremely easy to reuse in your own environment. For example, why write an NTP module when someone else has already done it? So you have this module, and it happens to be on github - but uh oh, your puppet code is already in git. How do you compose your current set of modules with the external modules you want to add?

### Case 2: Publishing your internal modules

You've taken the time to carefully build a set of modules, and they are wondrous pieces of art that you want to share with the world. However, you have a single git repository that contains all of your history, and you need to somehow extract this module from the rest of your code base so that you can throw it up on github. You may also receive fixes and updates to the community, so you need to be able to sync changes back down. What is one to do?

* * *

The first solution you'll probably come across is git submodules. A full explanation of git submodules is out of the scope of this blog; I'm mainly here to rant about them. However, there are plenty of [great resources][3] that explain submodules in detail. However, here's the short answer: git submodules allow you to nest git repositories, in a fashion. So in a top level git project, instead of having a file or directory tracked, you would track a _commit_.

So this seems like an easy enough solution to our problems, right? For case one, we just add the modules we want as a submodule, and that problem is taken care of! And for the second case, we can do horrible things with git-filter-branch to extract the history for a specific puppet module, put said module on github, and then submodule?

If this worked well, then I wouldn't be writing this blog.

Aside: Before we begin, note that this is a rant, so it's intentionally over the top. Please take the hyperbole with a grain of salt.

The fundamental issue with git submodules is that they are static. Very static. You are tracking specific commits with git submodules - not branches, not references, a single commit. If you add commits to a submodule, the parent project won't know. If you have a bunch of forks of a module, git submodules don't care. You have one remote repository, and you point to a single commit. Until you update the parent project, nothing changes.

As I'll explain later, there are cases where git submodules work. However, for your day to day operations, perhaps think twice. In this sort of environment, Git submodules suck with branches, they're worse with remotes, and they're easy to break.

## Doing it Wrong

Before we go any further, the following is an example of the workflow you'll adopt if you use git submodules. In this example, we'll do something simple - add a package declaration in the module package::virtual::ubuntu, and then use that declaration in another module, group::linux. In essence, two files, and around 4 lines of code. This should be a trivial change, right?
    
    
    cd ~/puppet
    git pull production
    git submodule update --init
    git checkout -b super_project_py_dev
    cd modules/public/package/manifests/virtual/
    git checkout -b package_module_py_dev
    vi ubuntu.pp
    git commit -am "add python3-dev package"
    git push origin package_module_py_dev
    cd ~/puppet/modules/internal/group/manifests
    git checkout -b group_module_py_dev
    vi linux.pp
    git commit -am "Add python3?-dev packages to linux group"
    git push origin group_module_py_dev
    cd ~/puppet
    git add modules/internal/group/
    git add modules/public/package
    git commit -am "Update superproject"
    git push origin super_project_py_dev
    

_Oh my god._

Notice that we're being good little git developers and creating different branches for each one of these. To merge these changes into production, we would have to do this.
    
    
    cd ~/puppet
    git pull production
    git submodule update --init
    cd modules/public/package/manifests/virtual/
    git merge origin/package_module_py_dev
    git push origin production
    cd ~/puppet/modules/internal/group/manifests
    git merge origin/group_module_py_dev
    git push origin production
    cd ~/puppet
    git add modules/internal/group/
    git add modules/public/package
    git commit -am "Update superproject"
    git push origin production
    

The example here was not exaggerated - the git submodule example was given by someone who was fed up with the complexities of git submodules. The entire thing is a mess to use, and that's not even discussing the ways it can break.

## Without Submodules

So, what are we actually trying to achieve? If we don't add git submodules, what would our workflow look like?
    
    
    cd ~/puppet
    git pull production
    git checkout -b super_project_py_dev
    cd modules/public/package/manifests/virtual/
    vi ubuntu.pp
    git commit -am "add python3-dev package"
    cd ~/puppet/modules/internal/group/manifests
    vi linux.pp
    git commit -am "Add python3?-dev packages to linux group"
    git push origin super_project_py_dev
    

To merge this…
    
    
    git checkout production
    git merge super_project_py_dev
    git push
    

This is a simple, straightforward approach that's following git best practices. If you know enough to use github, you can do this.

## Scripting around the limitations

Joe McDonagh wrote a [blog post][4] on scripting git submodules to make them less painful. Now, I'm personally too lazy to write my own blog posts, so instead I'm just going to go through and quote mine the comments that emphasize my point.

&gt; But one thing I've learned from heavy git sub-module usage, is that if you don't script all these things, you will forget a step and break something. You're also constantly repeating yourself, since you always require N+1 changes for N changes (you must update the git super-project, which updates the SHA's found in .gitmodules).

Now assuming you're using git correctly, and making small commits representing logical units of work N commits or something like that is fine. However, this is also going to take N pushes. If you're being efficient, you'll commit all the subprojects in one shot, so we hit N + 1 commits. However, if you're doing this incrementally, you may not hit this ideal case.

In addition, the fact that it's so fragile and cumbersome that you miss a step you'll break things, something is amiss. There are plenty of times where scripting away monotonous work is the best solution, but in this case you're scripting on top of a brittle framework. It seems that we're trying to shoehorn submodules into a use case that it was never meant for.

&gt; You may have noticed that I call a script named updatecheckout.sh above. This is the script for updating all the submodules because it's a serious PITA to do anything with sub-modules that isn't scripted. That includes just keeping your checkout up to date and on the proper branches:

All of this smells of an [antipattern][5]. Git submodules sound great initially, but the farther down this road that you go, the more work you're going to generate trying to work around the limitations of git submodules.

I've used Joe's code and I've chatted with him on #puppet, and I think he's a great guy. The fact that he's made this work in a reasonable way means he's above a lot of the crowd that is trying to use submodules and making things inoperable. However, this is still a horribly painful workflow.

## Difficulty of use aside, what's the harm?

### Your main project suddenly becomes useless.

If you want to see how your project as a whole has developed, git submodules will destroy that. Instead of seeing useful information in the log of the super project, you're going to see something somewhat like this:
    
    
    d61f574 Update submodule to increment git describe
    6b2ea1f updating submodule
    eb04ed4 Updating submodules after checking out the right branch in each folder
    59b93da Updating submodule pointers
    d648aaf Updating pointers for submodules.
    16db200 Updating submodule references in master project.
    37a1ea0 Add missing submodules
    3d4f244 Update packaging submodule
    83c2d9c Update packaging submodules.
    612da3b Remove old submodules
    da9ed6a Add Debian/Ubuntu packaging submodules.
    bc07065 Use absolute URL for submodules
    42c0c17 Add submodule for RPM packages
    

Well I don't know about you, but I sure can glean a lot of useful information from these logs!!! Admittedly, this is contrived - you could easily provide better commit messages. However, when you're just bumping a pointer, again, and again, and again, you're not really going to go for the best commit messages, are you? If you did, would that even be useful?

### You lose all visibility in submodules

Say you make two updates - one in a database module, one in a web application. I hope you never needed to see how those related, because when you switch to submodules you can't see diffs across submodules. You can't view diffs across projects, or logs, or walk through the history of the project as a whole. Each subproject is entirely isolated, so you have no project-wide view. This is quite important, as seeing how one change affects other modules is actually really important to understand the project as a whole - for instance, seeing how commits are interleaved to understand how one affects another. If you change how one module works and then have to change other modules because of this, these operations are synthetically disconnected due to the nature of submodules.

### Your workflow becomes very complex and hard to follow

The example given in the beginning of those post gives a pretty strong example of how complex and tedious submodules can be. If you're fairly comfortable with git and understand the object model well, then this is tolerable. However, if you're trying to introduce other people to git and your workflow, you will make their heads explode. This entire system is very unforgiving and unintuitive; getting started with git alone is hard enough and if you throw in this level of complexity, then people are just going to give up.

### Submodules don't play nice with multiple remotes

One of the major components of Git, and in fact one of its main strengths is that it is decentralized - while you can have remotes, tracking branches, and other things, you're not inherently tied to a specific remote repository.

Since git submodules have to have a remote repository specified, that model breaks down. If you want to do things like develop submodules against multiple repositories, you're going to be in pain since you constantly have to swap out remotes in your .gitmodules. This is a lot of bookkeeping and a lot of commits, and you're probably going to have to have merge conflicts on what amounts to metadata.

All of this clashes very badly with standard git workflows. Forking is supposed to be easy, branches are supposed to be lightweight, and repositories are supposed to be complete copies - and submodules prevent all of this.

### Submodules break easily, and submodules break badly

One of the major complaints against git submodules is that they're very fragile and easy to break. For instance, if you're doing some quick and dirty work in production, like adding a comment or something minute like that, and miss one step, suddenly your superproject can't be fully cloned.
    
    
    cd ~/puppet
    git pull production
    git submodule update --init
    cd modules/public/package/manifests/virtual/
    vi ubuntu.pp
    git commit -am "add python3-dev package"
    cd ~/puppet
    git add modules/internal/group/
    git commit -am "Update superproject"
    git push origin super_project_py_dev
    

Notice what was left out?

Yep, the git push of the submodule. You now have a super project referencing a commit that has not been pushed to the submodule, and that entire directory is just GONE. If anyone else tries to update their copy, Git will throw an error that the subproject commit cannot be found. While pushing straight into production isn't the greatest idea, it shouldn't completely fry the entire project.

Let's add more pain and see how a careless merge can make things explode far down the road. Recall from above that we branched a submodule and pushed to a branch, called package_module_py_dev, and updated the commit in the super project in a branch called super_project_py_dev. What if someone merged the super project and didn't merge the submodule?
    
    
    cd ~/puppet
    git pull production
    git submodule update --init
    git add modules/public/package
    git commit -am "Update superproject"
    git push origin production
    

Yep, we have a commit that is only in a branch. It might not even be in a central repository. If someone cleans up branches that got merged, then weeks down the road your super project will break with a missing commit and you'll have no immediate apparent reason why.

If you're using a workflow where a single inconspicuous error can cause catastrophic failures, then there's something definitely wrong.

### Git submodules will burn your house down

What's a blog post without hyperbole?

## What submodules are for

So, we've seen how badly git submodules fail. Why are they around? The short answer is that Linus wanted them and LINUS GETS WHAT LINUS WANTS. (Not really.) The longer answer that there are a number of use cases for them, and they all center around nested git modules that are much more static in nature and use.

One - you have a component or subproject where the project is undergoing extremely rapid change or is unstable, so you actually want to be locked to a specific commit for your own safety. Or, perhaps a project is breaking backwards compatibility of their API and you don't want to have to deal with that till they stabilize their code. In this case, git submodules, being reasonably static, are protecting you because of that static nature. You don't have to clone the outside component and switch to a specific branch or go to any of that hassle - things just work.

Two - you have a subproject or component that you're either vendoring or isn't being updated too often, and you just want an easy copy on hand. To provide an example, in my dot files, if there's a vim plugin I want I can just add it as a git submodule, and it's done. I don't care about the history. I don't need to be at the latest version. I don't plan on doing a lot of work on that code myself. Since this entire workflow is static, things work fine.

Three - There's a part of your repository that you're delegating to another party. Let's say you're paying someone to write a plugin for a project you're using, and you need to develop on the main codebase. In this case, the plugin repository is chiefly developed by the plugin developers, so they own the repo and periodically they'll tell you when to update submodule commits. Submodules are great for dividing responsibilities like this, assuming that there's not frequent updating.

## So what are we supposed to do?

There's a really cool thing called git subtrees, which is both a merge strategy and a command line tool. It's basically slapchop for git and breaking up git repositories. It's also a story for a different day. Stay tuned, folks.

* * *

Git submodules have a use, but they are absolutely _not_ a cure-all. They have very big limitations, and if you're using them with puppet, or with any rapidly changing codebase that needs to be cohesive, then you are blowing your foot off with a shotgun.

[1]: http://somethingsinistral.net/rethinking-puppet-deployment/
[2]: http://puppetlabs.com/blog/git-workflow-and-puppet-environments/
[3]: http://speirs.org/blog/2009/5/11/understanding-git-submodules.html
[4]: http://blog.thesilentpenguin.com/blog/2012/02/21/puppet-with-git-submodules-for-fun-and-profit/
[5]: http://en.wikipedia.org/wiki/Anti-pattern
