{{var a = [x: 1, y: 2, z: [5]]}}
        {{for i in [1,2]}}
        {{*a.z.e + i*}}
        {{a['z'][0]}}
        {{/for}}